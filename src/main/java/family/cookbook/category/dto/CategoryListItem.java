package family.cookbook.category.dto;

import java.util.UUID;

// List entry with the number of recipes using this category, so a client can warn before deleting
public record CategoryListItem(UUID id, String name, long recipeCount) {
}
