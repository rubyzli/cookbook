package family.cookbook.recipe.dto;

import family.cookbook.recipe.Recipe;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record RecipeDetail(
        UUID id,
        String name,
        String description,
        Integer servings,
        Integer prepTimeMinutes,
        Integer cookTimeMinutes,
        String instructions,
        String notes,
        String imageUrl,
        String createdBy,
        Instant createdAt,
        List<CategoryRef> categories,
        List<RecipeIngredientResponse> ingredients) {

    public static RecipeDetail from(Recipe recipe) {
        return new RecipeDetail(
                recipe.getId(),
                recipe.getName(),
                recipe.getDescription(),
                recipe.getServings(),
                recipe.getPrepTimeMinutes(),
                recipe.getCookTimeMinutes(),
                recipe.getInstructions(),
                recipe.getNotes(),
                recipe.getImageUrl(),
                recipe.getCreatedBy(),
                recipe.getCreatedAt(),
                CategoryRef.sortedFrom(recipe.getCategories()),
                recipe.getIngredients().stream().map(RecipeIngredientResponse::from).toList());
    }
}
