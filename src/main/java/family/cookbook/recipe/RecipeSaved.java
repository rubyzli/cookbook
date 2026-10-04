package family.cookbook.recipe;

import java.util.UUID;

// Published when a recipe is created or edited; listeners run once the change is committed
public record RecipeSaved(UUID recipeId) {
}
