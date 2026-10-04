package family.cookbook.translation;

import family.cookbook.recipe.Recipe;
import family.cookbook.recipe.RecipeIngredient;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.List;
import java.util.Objects;
import java.util.stream.Stream;

// The parts of a recipe that get translated, and a fingerprint of them. A translation stores the
// fingerprint of the original it was made from; if the original is edited later, they no longer match.
public final class SourceText {

    private SourceText() {
    }

    // Distinct ingredient group headings, in list order
    public static List<String> groups(Recipe recipe) {
        return recipe.getIngredients().stream()
                .map(RecipeIngredient::getGroup)
                .filter(Objects::nonNull)
                .distinct()
                .toList();
    }

    public static String hash(Recipe recipe) {
        String joined = Stream.concat(
                        Stream.of(recipe.getName(), recipe.getDescription(), recipe.getInstructions(), recipe.getNotes()),
                        groups(recipe).stream())
                .map(part -> part == null ? "" : part)
                .reduce((a, b) -> a + '\u0000' + b)
                .orElse("");
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256").digest(joined.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(digest);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 is always available", e);
        }
    }
}
