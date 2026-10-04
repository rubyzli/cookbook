package family.cookbook.recipe.dto;

import family.cookbook.recipe.RecipeIngredient;

import java.math.BigDecimal;
import java.util.UUID;

public record RecipeIngredientResponse(UUID ingredientId, String name, BigDecimal amount, String unit, String group) {

    static RecipeIngredientResponse from(RecipeIngredient line) {
        return new RecipeIngredientResponse(
                line.getIngredient().getId(),
                line.getIngredient().getName(),
                line.getAmount(),
                line.getUnit(),
                line.getGroup());
    }
}
