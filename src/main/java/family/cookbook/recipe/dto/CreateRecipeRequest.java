package family.cookbook.recipe.dto;

import jakarta.validation.constraints.NotBlank;

public record CreateRecipeRequest(@NotBlank String name) {
}
