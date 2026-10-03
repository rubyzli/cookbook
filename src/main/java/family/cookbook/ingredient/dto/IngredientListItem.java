package family.cookbook.ingredient.dto;

import java.util.UUID;

// List entry with the number of recipes using this ingredient, so a client can warn before deleting
public record IngredientListItem(UUID id, String name, long recipeCount) {
}
