package family.cookbook.nutrition;

import family.cookbook.recipe.RecipeSaved;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import java.util.UUID;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class AutomaticNutritionTest {

    private final NutritionService nutritionService = mock(NutritionService.class);
    private final AutomaticNutrition automaticNutrition = new AutomaticNutrition(nutritionService);
    private final UUID recipeId = UUID.randomUUID();

    @Test
    void estimatesWhenNeeded() {
        when(nutritionService.needsEstimate(recipeId)).thenReturn(true);

        automaticNutrition.recipeSaved(new RecipeSaved(recipeId));

        verify(nutritionService).estimate(recipeId);
    }

    @Test
    void leavesAnUpToDateEstimateAlone() {
        when(nutritionService.needsEstimate(recipeId)).thenReturn(false);

        automaticNutrition.recipeSaved(new RecipeSaved(recipeId));

        verify(nutritionService, never()).estimate(recipeId);
    }

    @Test
    void aFailureIsOnlyLogged() {
        when(nutritionService.needsEstimate(recipeId)).thenReturn(true);
        when(nutritionService.estimate(recipeId)).thenThrow(
                new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE, "busy"));

        automaticNutrition.recipeSaved(new RecipeSaved(recipeId));
    }
}
