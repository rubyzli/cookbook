package family.cookbook.recipe.dto;

import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.util.UUID;

// amount and unit are optional, for lines like "salt, to taste". group is the optional heading the
// line is listed under, e.g. "For the dough"; blank means none.
public record RecipeIngredientRequest(
        @NotNull UUID ingredientId,
        @PositiveOrZero @Digits(integer = 8, fraction = 2) BigDecimal amount,
        @Size(max = 50) String unit,
        @Size(max = 100) String group) {

    public RecipeIngredientRequest {
        group = group == null || group.isBlank() ? null : group.strip();
    }
}
