package family.cookbook.nutrition;

import family.cookbook.recipe.RecipeSaved;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionalEventListener;

// Estimates a recipe's nutrition after it's saved, when it has none yet or its ingredients changed.
// Runs in the background after the save is committed, like AutomaticTranslation.
@Component
public class AutomaticNutrition {

    private static final Logger log = LoggerFactory.getLogger(AutomaticNutrition.class);

    private final NutritionService nutritionService;

    public AutomaticNutrition(NutritionService nutritionService) {
        this.nutritionService = nutritionService;
    }

    @Async
    @TransactionalEventListener
    public void recipeSaved(RecipeSaved event) {
        if (!nutritionService.needsEstimate(event.recipeId())) return;
        try {
            nutritionService.estimate(event.recipeId());
        } catch (RuntimeException e) {
            // The button on the recipe page can still be used later
            log.warn("Couldn't estimate the nutrition of recipe {}: {}", event.recipeId(), e.getMessage());
        }
    }
}
