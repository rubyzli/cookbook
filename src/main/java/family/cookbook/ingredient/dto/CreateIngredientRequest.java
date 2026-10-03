package family.cookbook.ingredient.dto;

import jakarta.validation.constraints.NotBlank;

public record CreateIngredientRequest(@NotBlank String name) {
}
