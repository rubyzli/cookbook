package family.cookbook.recipe.dto;

import family.cookbook.recipe.RecipeIngredient;
import family.cookbook.translation.Localization;

import java.math.BigDecimal;
import java.util.Map;
import java.util.UUID;

public record RecipeIngredientResponse(UUID ingredientId, String name, BigDecimal amount, String unit, String group) {

    // groups: translated group headings by original heading (empty when showing the original)
    static RecipeIngredientResponse from(RecipeIngredient line, Localization localization, Map<String, String> groups) {
        String group = line.getGroup() == null ? null : groups.getOrDefault(line.getGroup(), line.getGroup());
        return new RecipeIngredientResponse(
                line.getIngredient().getId(),
                localization.nameOf(line.getIngredient()),
                line.getAmount(),
                line.getUnit(),
                group);
    }
}
