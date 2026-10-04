package family.cookbook.importer;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

import java.time.Duration;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

// Reads a recipe out of a web page. Most recipe sites describe each recipe in schema.org form for
// search engines (https://schema.org/Recipe), as JSON-LD or as microdata; without either, the
// page's own title, description and preview photo (Open Graph) are used.
public final class RecipePageParser {

    public record ParsedRecipe(
            boolean recipeData,
            String name,
            String description,
            Integer servings,
            Integer prepTimeMinutes,
            Integer cookTimeMinutes,
            List<String> steps,
            List<String> ingredientLines,
            List<String> imageUrls,
            List<String> categoryHints,
            String language) {
    }

    private static final Pattern FIRST_NUMBER = Pattern.compile("(\\d+)");

    private RecipePageParser() {
    }

    public static ParsedRecipe parse(String html, String pageUrl, ObjectMapper json) {
        Document page = Jsoup.parse(html, pageUrl);
        String language = Optional.ofNullable(page.selectFirst("html[lang]")).map(e -> e.attr("lang")).orElse(null);
        String siteName = meta(page, "og:site_name");

        Optional<JsonNode> jsonLd = findJsonLdRecipe(page, json);
        if (jsonLd.isPresent()) return fromJsonLd(jsonLd.get(), page, siteName, language);
        Element microdata = page.selectFirst("[itemtype~=(?i)schema\\.org/Recipe$]");
        if (microdata != null) return fromMicrodata(microdata, page, siteName, language);
        return fromPage(page, siteName, language);
    }

    // JSON-LD

    private static Optional<JsonNode> findJsonLdRecipe(Document page, ObjectMapper json) {
        for (Element script : page.select("script[type=application/ld+json]")) {
            try {
                Optional<JsonNode> found = findRecipe(json.readTree(script.data()), 0);
                if (found.isPresent()) return found;
            } catch (RuntimeException e) {
                // Broken JSON in one block shouldn't stop us looking at the others
            }
        }
        return Optional.empty();
    }

    // Recipes can sit in a list, in an "@graph", or as the "mainEntity" of a page
    private static Optional<JsonNode> findRecipe(JsonNode node, int depth) {
        if (node == null || depth > 4) return Optional.empty();
        if (node.isArray()) {
            for (JsonNode item : node.values()) {
                Optional<JsonNode> found = findRecipe(item, depth + 1);
                if (found.isPresent()) return found;
            }
            return Optional.empty();
        }
        if (!node.isObject()) return Optional.empty();
        if (isType(node.path("@type"), "Recipe")) return Optional.of(node);
        Optional<JsonNode> inGraph = findRecipe(node.get("@graph"), depth + 1);
        return inGraph.isPresent() ? inGraph : findRecipe(node.get("mainEntity"), depth + 1);
    }

    private static boolean isType(JsonNode type, String wanted) {
        if (type.isString()) return type.asString().equalsIgnoreCase(wanted) || type.asString().endsWith("/" + wanted);
        if (type.isArray()) return type.values().stream().anyMatch(t -> isType(t, wanted));
        return false;
    }

    private static ParsedRecipe fromJsonLd(JsonNode recipe, Document page, String siteName, String pageLanguage) {
        List<String> images = new ArrayList<>();
        collectImages(recipe.get("image"), images);
        if (images.isEmpty()) Optional.ofNullable(meta(page, "og:image")).ifPresent(images::add);
        List<String> ingredients = new ArrayList<>();
        JsonNode ingredientNode = recipe.has("recipeIngredient") ? recipe.get("recipeIngredient") : recipe.get("ingredients");
        for (String line : strings(ingredientNode)) {
            String cleaned = clean(line);
            if (!cleaned.isEmpty()) ingredients.add(cleaned);
        }
        List<String> steps = new ArrayList<>();
        collectSteps(recipe.get("recipeInstructions"), steps);
        List<String> categories = new ArrayList<>(strings(recipe.get("recipeCategory")));
        categories.addAll(keywords(recipe.get("keywords")));
        String language = text(recipe.get("inLanguage"));
        return new ParsedRecipe(
                true,
                withoutSiteName(firstNonBlank(text(recipe.get("name")), meta(page, "og:title"), page.title()), siteName),
                firstNonBlank(text(recipe.get("description")), meta(page, "og:description"), meta(page, "description")),
                servings(recipe.get("recipeYield")),
                minutes(text(recipe.get("prepTime"))),
                minutes(text(recipe.get("cookTime"))),
                steps,
                ingredients,
                images,
                categories,
                language == null ? pageLanguage : language);
    }

    private static void collectImages(JsonNode node, List<String> into) {
        if (node == null) return;
        if (node.isString()) {
            into.add(node.asString());
        } else if (node.isArray()) {
            node.values().forEach(item -> collectImages(item, into));
        } else if (node.isObject()) {
            Optional.ofNullable(text(node.has("url") ? node.get("url") : node.get("contentUrl"))).ifPresent(into::add);
        }
    }

    // Steps come as one text, a list of texts, HowToStep objects, or HowToSections of steps
    private static void collectSteps(JsonNode node, List<String> into) {
        if (node == null) return;
        if (node.isString()) {
            for (String line : clean(node.asString().replaceAll("(?i)<br\\s*/?>|</p>|</li>", "\n")).split("\\n")) {
                if (!line.isBlank()) into.add(line.strip());
            }
        } else if (node.isArray()) {
            node.values().forEach(item -> collectSteps(item, into));
        } else if (node.isObject()) {
            if (isType(node.path("@type"), "HowToSection")) {
                Optional.ofNullable(text(node.get("name"))).map(name -> name.endsWith(":") ? name : name + ":").ifPresent(into::add);
                collectSteps(node.get("itemListElement"), into);
            } else if (node.has("text")) {
                collectSteps(node.get("text"), into);
            } else if (node.has("itemListElement")) {
                collectSteps(node.get("itemListElement"), into);
            } else {
                collectSteps(node.get("name"), into);
            }
        }
    }

    private static List<String> strings(JsonNode node) {
        List<String> result = new ArrayList<>();
        if (node == null) return result;
        if (node.isArray()) {
            node.values().forEach(item -> Optional.ofNullable(text(item)).ifPresent(result::add));
        } else {
            Optional.ofNullable(text(node)).ifPresent(result::add);
        }
        return result;
    }

    private static List<String> keywords(JsonNode node) {
        List<String> result = new ArrayList<>();
        for (String value : strings(node)) {
            for (String part : value.split(",")) {
                if (!part.isBlank()) result.add(part.strip());
            }
        }
        return result;
    }

    private static String text(JsonNode node) {
        if (node == null || node.isNull() || node.isArray() || node.isObject()) return null;
        String value = clean(node.asString());
        return value.isEmpty() ? null : value;
    }

    // Microdata

    private static ParsedRecipe fromMicrodata(Element recipe, Document page, String siteName, String language) {
        List<String> ingredients = recipe.select("[itemprop=recipeIngredient], [itemprop=ingredients]").stream()
                .map(e -> clean(e.text())).filter(s -> !s.isEmpty()).toList();
        List<String> steps = new ArrayList<>();
        for (Element instructions : recipe.select("[itemprop=recipeInstructions]")) {
            var items = instructions.select("li");
            if (items.isEmpty()) {
                steps.add(clean(instructions.text()));
            } else {
                items.forEach(li -> steps.add(clean(li.text())));
            }
        }
        steps.removeIf(String::isEmpty);
        List<String> images = new ArrayList<>();
        Element image = recipe.selectFirst("[itemprop=image]");
        if (image != null) {
            String src = firstNonBlank(image.absUrl("src"), image.absUrl("content"), image.absUrl("href"), image.attr("content"));
            if (src != null) images.add(src);
        }
        if (images.isEmpty()) Optional.ofNullable(meta(page, "og:image")).ifPresent(images::add);
        return new ParsedRecipe(
                true,
                withoutSiteName(firstNonBlank(itemprop(recipe, "name"), meta(page, "og:title"), page.title()), siteName),
                firstNonBlank(itemprop(recipe, "description"), meta(page, "og:description")),
                servingsFromText(itemprop(recipe, "recipeYield")),
                minutes(itemprop(recipe, "prepTime")),
                minutes(itemprop(recipe, "cookTime")),
                steps,
                ingredients,
                images,
                Optional.ofNullable(itemprop(recipe, "recipeCategory")).map(List::of).orElse(List.of()),
                language);
    }

    private static String itemprop(Element scope, String name) {
        Element element = scope.selectFirst("[itemprop=" + name + "]");
        if (element == null) return null;
        String value = firstNonBlank(element.attr("content"), element.attr("datetime"), element.text());
        return value == null ? null : clean(value);
    }

    // No recipe data: whatever the page says about itself

    private static ParsedRecipe fromPage(Document page, String siteName, String language) {
        List<String> images = new ArrayList<>();
        Optional.ofNullable(meta(page, "og:image")).ifPresent(images::add);
        return new ParsedRecipe(false,
                withoutSiteName(firstNonBlank(meta(page, "og:title"), page.title()), siteName),
                firstNonBlank(meta(page, "og:description"), meta(page, "description")),
                null, null, null, List.of(), List.of(), images, List.of(), language);
    }

    // Helpers

    private static String meta(Document page, String name) {
        Element element = page.selectFirst("meta[property=" + name + "], meta[name=" + name + "]");
        if (element == null) return null;
        String content = clean(element.attr("content"));
        return content.isEmpty() ? null : content;
    }

    // Decodes entities like &nbsp;, drops any HTML tags and tidies spaces
    static String clean(String text) {
        if (text == null) return "";
        String withBreaks = text.replace("\r\n", "\n");
        if (withBreaks.indexOf('<') < 0 && withBreaks.indexOf('&') < 0) {
            return withBreaks.replace(' ', ' ').replaceAll("[ \\t]+", " ").strip();
        }
        StringBuilder lines = new StringBuilder();
        for (String line : withBreaks.split("\\n")) {
            if (!lines.isEmpty()) lines.append('\n');
            lines.append(Jsoup.parse(line).text().replace(' ', ' ').replaceAll("[ \\t]+", " ").strip());
        }
        return lines.toString().strip();
    }

    // "Rakott krumpli | Mindmegette.hu" -> "Rakott krumpli"
    static String withoutSiteName(String title, String siteName) {
        if (title == null) return null;
        for (String separator : List.of(" | ", " – ", " - ", " :: ")) {
            int at = title.lastIndexOf(separator);
            if (at > 0) {
                String suffix = title.substring(at + separator.length()).strip();
                if (suffix.contains(".") || (siteName != null && suffix.equalsIgnoreCase(siteName))) {
                    return title.substring(0, at).strip();
                }
            }
        }
        return title;
    }

    // ISO 8601 durations ("PT1H30M") as minutes; also "45 perc" / "45 min"
    static Integer minutes(String value) {
        if (value == null || value.isBlank()) return null;
        try {
            long minutes = Duration.parse(value.strip().toUpperCase(Locale.ROOT)).toMinutes();
            return minutes > 0 ? (int) minutes : null;
        } catch (DateTimeParseException e) {
            Matcher number = FIRST_NUMBER.matcher(value);
            return number.find() && Integer.parseInt(number.group(1)) > 0 ? Integer.parseInt(number.group(1)) : null;
        }
    }

    private static Integer servings(JsonNode node) {
        if (node == null) return null;
        if (node.isNumber()) return node.asInt() > 0 ? node.asInt() : null;
        if (node.isArray()) {
            for (JsonNode item : node.values()) {
                Integer found = servings(item);
                if (found != null) return found;
            }
            return null;
        }
        return servingsFromText(text(node));
    }

    private static Integer servingsFromText(String text) {
        if (text == null) return null;
        Matcher number = FIRST_NUMBER.matcher(text);
        if (!number.find()) return null;
        int value = Integer.parseInt(number.group(1));
        return value > 0 && value < 1000 ? value : null;
    }

    private static String firstNonBlank(String... values) {
        for (String value : values) {
            if (value != null && !value.isBlank()) return value.strip();
        }
        return null;
    }
}
