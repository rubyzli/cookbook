package family.cookbook.translation;

import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import java.util.Locale;
import java.util.Optional;
import java.util.Set;

// The languages recipes can be written and translated in, as lower-case ISO 639-1 codes
public final class Languages {

    public static final Set<String> SUPPORTED = Set.of("en", "de", "hu");
    public static final String DEFAULT = "hu";
    // For @Pattern on request fields
    public static final String PATTERN = "en|de|hu";

    private Languages() {
    }

    // The supported language a request asks for (from Accept-Language), if any
    public static Optional<String> of(Locale locale) {
        if (locale == null) return Optional.empty();
        String code = locale.getLanguage().toLowerCase(Locale.ROOT);
        return SUPPORTED.contains(code) ? Optional.of(code) : Optional.empty();
    }

    // The first supported language in an Accept-Language header such as "de-AT,de;q=0.9,en;q=0.8"
    public static Optional<String> fromHeader(String acceptLanguage) {
        if (acceptLanguage == null || acceptLanguage.isBlank()) return Optional.empty();
        try {
            return Locale.LanguageRange.parse(acceptLanguage).stream()
                    .map(range -> range.getRange().split("-")[0].toLowerCase(Locale.ROOT))
                    .filter(SUPPORTED::contains)
                    .findFirst();
        } catch (IllegalArgumentException e) {
            return Optional.empty();
        }
    }

    // A language code from a URL, rejected with 400 if unsupported
    public static String require(String code) {
        String normalized = code == null ? "" : code.toLowerCase(Locale.ROOT);
        if (!SUPPORTED.contains(normalized)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Unsupported language: " + code);
        }
        return normalized;
    }
}
