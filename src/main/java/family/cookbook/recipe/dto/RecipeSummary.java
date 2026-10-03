package family.cookbook.recipe.dto;

import family.cookbook.recipe.Recipe;

import java.util.List;
import java.util.UUID;

// List view: no instructions or ingredients
public record RecipeSummary(
        UUID id,
        String name,
        String description,
        String imageUrl,
        Integer servings,
        Integer prepTimeMinutes,
        Integer cookTimeMinutes,
        List<CategoryRef> categories) {

    public static RecipeSummary from(Recipe recipe) {
        return new RecipeSummary(
                recipe.getId(),
                recipe.getName(),
                recipe.getDescription(),
                recipe.getImageUrl(),
                recipe.getServings(),
                recipe.getPrepTimeMinutes(),
                recipe.getCookTimeMinutes(),
                CategoryRef.sortedFrom(recipe.getCategories()));
    }
}
