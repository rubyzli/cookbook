package family.cookbook.importer;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;

// Splits an ingredient line from a recipe page ("25 dkg liszt", "2 EL Zucker", "1 ½ cups of flour")
// into amount, unit and ingredient. Sites only give the line as text, so this is a best guess the
// person importing checks in the form.
public final class IngredientLineParser {

    public record Line(BigDecimal amount, String unit, String name) {
    }

    // Unit words and phrases in Hungarian, German and English, matched case-insensitively
    private static final List<String> UNITS = Stream.of(
                    // Hungarian
                    "g", "gr", "gramm", "dkg", "dag", "kg", "mg", "l", "liter", "dl", "cl", "ml",
                    "ek", "evőkanál", "evőkanálnyi", "tk", "teáskanál", "teáskanálnyi", "kk", "kiskanál", "kávéskanál",
                    "mk", "mokkáskanál", "kanál", "csapott kanál", "púpozott kanál", "csapott evőkanál", "púpozott evőkanál",
                    "db", "darab", "fej", "kis fej", "nagy fej", "közepes fej", "gerezd", "szál", "csomag", "cs", "tasak",
                    "zacskó", "csipet", "csipetnyi", "doboz", "konzerv", "bögre", "pohár", "szelet", "marék", "csokor",
                    "késhegynyi", "ízlés szerint", "tetszés szerint",
                    // German
                    "gramm", "kilogramm", "el", "esslöffel", "tl", "teelöffel", "msp", "msp.", "prise", "prisen",
                    "stk", "stk.", "stück", "pck", "pck.", "päckchen", "packung", "bund", "zehe", "zehen", "dose", "dosen",
                    "becher", "tasse", "tassen", "scheibe", "scheiben", "handvoll", "nach belieben", "etwas",
                    // English
                    "gram", "grams", "kilogram", "kilograms", "tbsp", "tablespoon", "tablespoons", "tsp", "teaspoon",
                    "teaspoons", "cup", "cups", "oz", "ounce", "ounces", "lb", "lbs", "pound", "pounds", "pinch", "pinches",
                    "clove", "cloves", "can", "cans", "slice", "slices", "piece", "pieces", "pcs", "stick", "sticks",
                    "handful", "bunch", "package", "packages", "packet", "packets", "sprig", "sprigs", "dash", "to taste")
            .distinct()
            // Longest first, so "púpozott kanál" wins over "kanál"
            .sorted(Comparator.comparingInt(String::length).reversed())
            .toList();

    // Phrases that mean "as much as you like", also found at the end of a line ("Só ízlés szerint")
    private static final List<String> TO_TASTE = List.of("ízlés szerint", "tetszés szerint", "nach belieben", "to taste");

    private static final Map<Character, String> FRACTIONS = Map.of(
            '½', "1/2", '¼', "1/4", '¾', "3/4", '⅓', "1/3", '⅔', "2/3", '⅛', "1/8");

    // Fractions first, or "1/2" would stop at the "1"
    private static final String NUMBER = "\\d+\\s+\\d+/\\d+|\\d+/\\d+|\\d+(?:[.,]\\d+)?";
    private static final Pattern AMOUNT = Pattern.compile(
            "^(" + NUMBER + ")(?:\\s*[-–]\\s*(" + NUMBER + "))?\\s*");

    private IngredientLineParser() {
    }

    public static Line parse(String text, String language) {
        String line = normalize(text);
        BigDecimal amount = null;
        String range = null;
        String rest = line;

        Matcher number = AMOUNT.matcher(line);
        if (number.find() && number.end() > 0) {
            if (number.group(2) != null) {
                // "2-3 ek" has no single amount; the range stays readable in the unit
                range = number.group(1) + "–" + number.group(2);
            } else {
                amount = toNumber(number.group(1));
                // Some sites write "0 ízlés szerint" for "to taste"
                if (amount != null && amount.signum() == 0) amount = null;
            }
            rest = line.substring(number.end());
        }

        String unit = null;
        String lower = rest.toLowerCase(Locale.ROOT);
        for (String candidate : UNITS) {
            if (lower.startsWith(candidate) && (lower.length() == candidate.length()
                    || !Character.isLetter(lower.charAt(candidate.length())))) {
                // Keep the site's own spelling, e.g. "EL"
                unit = rest.substring(0, candidate.length());
                rest = rest.substring(candidate.length()).replaceFirst("^\\.?\\s*", "");
                break;
            }
        }
        // Units that are only units after a number: "el" or "can" alone start a normal word
        if (unit != null && amount == null && range == null && !TO_TASTE.contains(unit.toLowerCase(Locale.ROOT))
                && unit.length() <= 3 && rest.isEmpty()) {
            rest = unit;
            unit = null;
        }

        String name = rest.replaceFirst("^(?i)of\\s+", "").replaceFirst("^[,;:\\-–]\\s*", "").strip();
        String nameLower = name.toLowerCase(Locale.ROOT);
        for (String phrase : TO_TASTE) {
            if (unit == null && nameLower.endsWith(" " + phrase)) {
                unit = name.substring(name.length() - phrase.length());
                name = name.substring(0, name.length() - phrase.length()).replaceFirst("[,\\s]+$", "");
                break;
            }
        }
        if (range != null) unit = unit == null ? range : range + " " + unit;
        if (name.isEmpty()) name = line;
        return new Line(amount, limit(unit, 50), limit(lowerFirstLetter(name, language), 255));
    }

    private static String normalize(String text) {
        StringBuilder out = new StringBuilder();
        for (char c : text.replace(' ', ' ').strip().toCharArray()) {
            String fraction = FRACTIONS.get(c);
            if (fraction == null) {
                out.append(c);
            } else {
                // "1½" becomes "1 1/2"
                if (!out.isEmpty() && Character.isDigit(out.charAt(out.length() - 1))) out.append(' ');
                out.append(fraction);
            }
        }
        return out.toString().replaceAll("\\s+", " ").strip();
    }

    // "1,5", "1.5", "1/2" and "1 1/2"; amounts are stored with two decimals
    static BigDecimal toNumber(String text) {
        try {
            String[] parts = text.strip().split("\\s+");
            BigDecimal total = BigDecimal.ZERO;
            for (String part : parts) {
                if (part.contains("/")) {
                    String[] fraction = part.split("/");
                    total = total.add(new BigDecimal(fraction[0]).divide(new BigDecimal(fraction[1]), 2, RoundingMode.HALF_UP));
                } else {
                    total = total.add(new BigDecimal(part.replace(',', '.')));
                }
            }
            return total.setScale(Math.min(2, Math.max(0, total.stripTrailingZeros().scale())), RoundingMode.HALF_UP);
        } catch (NumberFormatException | ArithmeticException e) {
            return null;
        }
    }

    // Hungarian and English ingredient names are lower case in this cookbook ("Burgonya" -> "burgonya");
    // German nouns keep their capital
    private static String lowerFirstLetter(String name, String language) {
        if ("de".equals(language) || name.length() < 2) return name;
        if (Character.isUpperCase(name.charAt(0)) && Character.isLowerCase(name.charAt(1))) {
            return Character.toLowerCase(name.charAt(0)) + name.substring(1);
        }
        return name;
    }

    private static String limit(String text, int max) {
        return text == null || text.length() <= max ? text : text.substring(0, max).strip();
    }
}
