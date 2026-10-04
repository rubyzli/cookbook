package family.cookbook.recipe.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;

import java.util.List;
import java.util.UUID;

// Used for both create and update; an update replaces every field, categories and ingredients included
public record RecipeRequest(
        @NotBlank @Size(max = 255) String name,
        @Size(max = 255) String description,
        @Positive Integer servings,
        @PositiveOrZero Integer prepTimeMinutes,
        @PositiveOrZero Integer cookTimeMinutes,
        String instructions,
        String notes,
        @Size(max = 255) String imageUrl,
        List<@NotNull UUID> categoryIds,
        List<@NotNull @Valid RecipeIngredientRequest> ingredients) {

    public RecipeRequest {
        categoryIds = categoryIds == null ? List.of() : categoryIds;
        ingredients = ingredients == null ? List.of() : ingredients;
    }
}
