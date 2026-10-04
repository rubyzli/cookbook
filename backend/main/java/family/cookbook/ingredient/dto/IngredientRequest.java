package family.cookbook.ingredient.dto;

import family.cookbook.translation.Languages;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

// Used for both create and rename. language: what the name is written in; Hungarian when left out
// on create, ignored on rename.
public record IngredientRequest(@NotBlank @Size(max = 255) String name, @Pattern(regexp = Languages.PATTERN) String language) {
}
