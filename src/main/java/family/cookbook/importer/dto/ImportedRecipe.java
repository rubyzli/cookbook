package family.cookbook.importer.dto;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

// A recipe read from a web page, for filling in the recipe form; nothing is saved yet.
// recipeFound: the page had recipe data (otherwise only title, description and photo).
// warnings: NO_RECIPE_DATA, PHOTO_NOT_DOWNLOADED (the photo is then linked instead of saved).
public record ImportedRecipe(
        String name,
        String description,
        Integer servings,
        Integer prepTimeMinutes,
        Integer cookTimeMinutes,
        String instructions,
        String imageUrl,
        String sourceUrl,
        String language,
        List<UUID> categoryIds,
        List<Ingredient> ingredients,
        boolean recipeFound,
        List<String> warnings) {

    public record Ingredient(BigDecimal amount, String unit, String name) {
    }
}
