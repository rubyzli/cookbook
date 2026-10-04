package family.cookbook.importer;

import family.cookbook.category.Category;
import family.cookbook.category.CategoryRepository;
import family.cookbook.image.ImageShrinker;
import family.cookbook.image.ImageStorage;
import family.cookbook.importer.RecipePageParser.ParsedRecipe;
import family.cookbook.importer.dto.ImportedRecipe;
import family.cookbook.translation.Languages;
import family.cookbook.translation.TranslationLookup;
import family.cookbook.translation.TranslationLookup.TranslatedName;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import tools.jackson.databind.ObjectMapper;

import java.net.URI;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
public class RecipeImportService {

    private static final Pattern CHARSET = Pattern.compile("(?i)charset=\"?([\\w.-]+)");

    private final PageFetcher fetcher;
    private final ImageStorage images;
    private final CategoryRepository categories;
    private final TranslationLookup translations;
    private final ObjectMapper json;

    public RecipeImportService(PageFetcher fetcher, ImageStorage images, CategoryRepository categories,
                               TranslationLookup translations, ObjectMapper json) {
        this.fetcher = fetcher;
        this.images = images;
        this.categories = categories;
        this.translations = translations;
        this.json = json;
    }

    // siteLanguage: the visitor's language, used when the page doesn't say (or isn't en/de/hu)
    @Transactional(readOnly = true)
    public ImportedRecipe importFrom(String url, Optional<String> siteLanguage) {
        URI uri = PublicAddresses.parseWebUrl(url);
        PageFetcher.Download page = fetcher.fetchPage(uri, siteLanguage.orElse(null));
        ParsedRecipe parsed = RecipePageParser.parse(decode(page), page.finalUri().toString(), json);
        if (parsed.name() == null && !parsed.recipeData()) {
            throw new RecipeImportException(HttpStatus.UNPROCESSABLE_CONTENT, "NO_RECIPE_DATA",
                    "No recipe was found on that page");
        }

        String language = Optional.ofNullable(parsed.language())
                .map(code -> code.toLowerCase(Locale.ROOT).split("[-_]")[0])
                .filter(Languages.SUPPORTED::contains)
                .or(() -> siteLanguage)
                .orElse(Languages.DEFAULT);

        List<String> warnings = new ArrayList<>();
        if (!parsed.recipeData()) warnings.add("NO_RECIPE_DATA");
        String imageUrl = savePhoto(parsed.imageUrls(), page.finalUri(), warnings);

        List<ImportedRecipe.Ingredient> ingredients = parsed.ingredientLines().stream()
                .map(line -> IngredientLineParser.parse(line, language))
                .map(line -> new ImportedRecipe.Ingredient(line.amount(), line.unit(), line.name()))
                .toList();

        return new ImportedRecipe(
                limit(parsed.name(), 255),
                shorten(parsed.description(), 255),
                parsed.servings(),
                parsed.prepTimeMinutes(),
                parsed.cookTimeMinutes(),
                parsed.steps().isEmpty() ? null : String.join("\n", parsed.steps()),
                imageUrl,
                limit(page.finalUri().toString(), 1000),
                language,
                matchCategories(parsed.categoryHints()),
                ingredients,
                parsed.recipeData(),
                warnings);
    }

    // Saved into the images folder so the recipe keeps its photo if the site changes; if that
    // fails, the photo is linked instead
    private String savePhoto(List<String> candidates, URI page, List<String> warnings) {
        if (candidates.isEmpty()) return null;
        String link = page.resolve(candidates.get(0).replace(" ", "%20")).toString();
        try {
            URI uri = PublicAddresses.parseWebUrl(link);
            return images.store(ImageShrinker.shrink(fetcher.fetchImage(uri).body()));
        } catch (RecipeImportException | ResponseStatusException | IllegalArgumentException e) {
            warnings.add("PHOTO_NOT_DOWNLOADED");
            return link.length() <= 255 ? link : null;
        }
    }

    // A site's category or keywords that match one of ours, in any language ("Saláta", "Salat")
    private List<UUID> matchCategories(List<String> hints) {
        if (hints.isEmpty()) return List.of();
        Map<String, UUID> byName = new HashMap<>();
        Map<UUID, Map<String, TranslatedName>> translated = translations.allCategoryNames();
        for (Category category : categories.findAll()) {
            byName.put(normalize(category.getName()), category.getId());
            translated.getOrDefault(category.getId(), Map.of()).values()
                    .forEach(name -> byName.putIfAbsent(normalize(name.name()), category.getId()));
        }
        LinkedHashSet<UUID> matched = new LinkedHashSet<>();
        for (String hint : hints) {
            Optional.ofNullable(byName.get(normalize(hint))).ifPresent(matched::add);
        }
        return List.copyOf(matched);
    }

    // Pages say their encoding in the Content-Type header or in a <meta> tag; UTF-8 otherwise
    private static String decode(PageFetcher.Download page) {
        Charset charset = StandardCharsets.UTF_8;
        String declared = Optional.ofNullable(page.contentType()).orElse("");
        String head = new String(page.body(), 0, Math.min(page.body().length, 4096), StandardCharsets.ISO_8859_1);
        Matcher match = CHARSET.matcher(declared);
        if (!match.find()) match = CHARSET.matcher(head);
        if (match.find(0)) {
            try {
                charset = Charset.forName(match.group(1));
            } catch (IllegalArgumentException ignored) {
                // Unknown name: stay with UTF-8
            }
        }
        return new String(page.body(), charset);
    }

    private static String normalize(String text) {
        return text.strip().toLowerCase(Locale.ROOT);
    }

    private static String limit(String text, int max) {
        return text == null || text.length() <= max ? text : text.substring(0, max).strip();
    }

    // Cut at a word boundary, marking the cut with "…"
    private static String shorten(String text, int max) {
        if (text == null || text.length() <= max) return text;
        String cut = text.substring(0, max - 1);
        int space = cut.lastIndexOf(' ');
        return (space > max / 2 ? cut.substring(0, space) : cut).strip() + "…";
    }
}
