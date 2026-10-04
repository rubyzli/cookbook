package family.cookbook.nutrition;

import java.util.List;

// Estimates the nutrition of a recipe from its ingredient lines. The Claude implementation is used
// when an API key is configured; tests use a fake.
public interface NutritionEstimator {

    boolean isEnabled();

    // ingredientLines are as written, e.g. "25 dkg liszt", with group headings as "A tésztához:"
    NutritionEstimate estimate(String recipeName, String language, List<String> ingredientLines);
}
