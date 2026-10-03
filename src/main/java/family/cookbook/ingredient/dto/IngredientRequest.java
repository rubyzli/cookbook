package family.cookbook.ingredient.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

// Used for both create and rename
public record IngredientRequest(@NotBlank @Size(max = 255) String name) {
}
