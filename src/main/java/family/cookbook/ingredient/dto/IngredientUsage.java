package family.cookbook.ingredient.dto;

import java.util.UUID;

// A ingredient with the number of recipes using it, as loaded from the database
public record IngredientUsage(UUID id, String name, String language, long recipeCount) {
}
