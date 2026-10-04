package family.cookbook.nutrition;

import family.cookbook.ingredient.Ingredient;
import family.cookbook.recipe.Recipe;
import family.cookbook.recipe.RecipeIngredient;
import family.cookbook.recipe.RecipeRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class NutritionServiceTest {

    // Returns a fixed estimate and records the lines it was asked about
    static class FakeEstimator implements NutritionEstimator {
        final List<List<String>> calls = new ArrayList<>();
        boolean enabled = true;

        @Override
        public boolean isEnabled() {
            return enabled;
        }

        @Override
        public NutritionEstimate estimate(String recipeName, String language, List<String> ingredientLines) {
            calls.add(ingredientLines);
            return new NutritionEstimate(1200, 40, 150, 45);
        }
    }

    private final RecipeRepository recipeRepository = mock(RecipeRepository.class);
    private final RecipeNutritionRepository nutritionRepository = mock(RecipeNutritionRepository.class);
    private final FakeEstimator estimator = new FakeEstimator();
    private final NutritionService service = new NutritionService(recipeRepository, nutritionRepository, estimator);
    private Recipe recipe;

    @BeforeEach
    void setUp() {
        Ingredient flour = new Ingredient("liszt", "hu");
        Ingredient egg = new Ingredient("tojás", "hu");
        Ingredient salt = new Ingredient("só", "hu");
        recipe = new Recipe("Pogácsa");
        recipe.setId(UUID.randomUUID());
        recipe.replaceIngredients(List.of(
                new RecipeIngredient(recipe, flour, new BigDecimal("25.00"), "dkg", "A tésztához", 0),
                new RecipeIngredient(recipe, egg, new BigDecimal("1.5"), "db", "A tésztához", 1),
                new RecipeIngredient(recipe, salt, null, null, null, 2)));
        when(recipeRepository.findWithDetailsById(recipe.getId())).thenReturn(Optional.of(recipe));
        when(nutritionRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
    }

    @Test
    void writesIngredientLinesWithGroupHeadings() {
        assertThat(NutritionService.ingredientLines(recipe))
                .containsExactly("A tésztához:", "25 dkg liszt", "1.5 db tojás", "só");
    }

    @Test
    void estimateStoresTheNumbersWithTheHashOfTheLines() {
        NutritionResponse response = service.estimate(recipe.getId()).orElseThrow();

        assertThat(response.estimate()).isEqualTo(new NutritionEstimate(1200, 40, 150, 45));
        assertThat(response.outdated()).isFalse();
        assertThat(response.estimatedAt()).isNotNull();
        assertThat(estimator.calls).hasSize(1);
    }

    @Test
    void aRecipeWithoutIngredientsHasNothingToAsk() {
        recipe.replaceIngredients(List.of());

        NutritionResponse response = service.estimate(recipe.getId()).orElseThrow();

        assertThat(response.estimate()).isEqualTo(new NutritionEstimate(0, 0, 0, 0));
        assertThat(estimator.calls).isEmpty();
    }

    @Test
    void anEstimateIsOutdatedOnceTheIngredientsChange() {
        RecipeNutrition stored = storedEstimate(List.of("10 dkg liszt"));

        assertThat(service.getNutrition(recipe.getId()).orElseThrow().outdated()).isTrue();
        assertThat(service.needsEstimate(recipe.getId())).isTrue();

        stored.update(stored.getEstimate(), NutritionService.hash(NutritionService.ingredientLines(recipe)));
        assertThat(service.getNutrition(recipe.getId()).orElseThrow().outdated()).isFalse();
        assertThat(service.needsEstimate(recipe.getId())).isFalse();
    }

    @Test
    void aRecipeWithoutAnEstimateNeedsOneWhenEstimatesAreSetUp() {
        when(nutritionRepository.findById(recipe.getId())).thenReturn(Optional.empty());

        assertThat(service.needsEstimate(recipe.getId())).isTrue();
        NutritionResponse response = service.getNutrition(recipe.getId()).orElseThrow();
        assertThat(response.estimate()).isNull();
        assertThat(response.enabled()).isTrue();

        estimator.enabled = false;
        assertThat(service.needsEstimate(recipe.getId())).isFalse();
    }

    @Test
    void unknownRecipesAreEmpty() {
        UUID unknown = UUID.randomUUID();
        when(recipeRepository.findWithDetailsById(unknown)).thenReturn(Optional.empty());

        assertThat(service.getNutrition(unknown)).isEmpty();
        assertThat(service.estimate(unknown)).isEmpty();
        assertThat(service.needsEstimate(unknown)).isFalse();
    }

    private RecipeNutrition storedEstimate(List<String> lines) {
        RecipeNutrition stored = new RecipeNutrition(recipe.getId());
        stored.update(new NutritionEstimate(500, 10, 80, 5), NutritionService.hash(lines));
        when(nutritionRepository.findById(recipe.getId())).thenReturn(Optional.of(stored));
        return stored;
    }
}
