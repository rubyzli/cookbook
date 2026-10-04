package family.cookbook.category.dto;

import java.util.UUID;

// A category with the number of recipes using it, as loaded from the database
public record CategoryUsage(UUID id, String name, String language, long recipeCount) {
}
