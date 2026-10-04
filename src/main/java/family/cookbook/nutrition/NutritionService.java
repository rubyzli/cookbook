package family.cookbook.nutrition;

import family.cookbook.recipe.Recipe;
import family.cookbook.recipe.RecipeIngredient;
import family.cookbook.recipe.RecipeRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.HexFormat;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

@Service
public class NutritionService {

    private final RecipeRepository recipeRepository;
    private final RecipeNutritionRepository nutritionRepository;
    private final NutritionEstimator estimator;

    public NutritionService(RecipeRepository recipeRepository, RecipeNutritionRepository nutritionRepository,
                            NutritionEstimator estimator) {
        this.recipeRepository = recipeRepository;
        this.nutritionRepository = nutritionRepository;
        this.estimator = estimator;
    }

    // Empty if the recipe doesn't exist
    @Transactional(readOnly = true)
    public Optional<NutritionResponse> getNutrition(UUID recipeId) {
        return recipeRepository.findWithDetailsById(recipeId).map(recipe -> response(recipe,
                nutritionRepository.findById(recipeId).orElse(null)));
    }

    // Makes a new estimate now, replacing any earlier one. Empty if the recipe doesn't exist.
    @Transactional
    public Optional<NutritionResponse> estimate(UUID recipeId) {
        Optional<Recipe> found = recipeRepository.findWithDetailsById(recipeId);
        if (found.isEmpty()) {
            return Optional.empty();
        }
        Recipe recipe = found.get();
        List<String> lines = ingredientLines(recipe);
        NutritionEstimate estimate = lines.isEmpty()
                ? new NutritionEstimate(0, 0, 0, 0)
                : estimator.estimate(recipe.getName(), recipe.getLanguage(), lines);
        RecipeNutrition nutrition = nutritionRepository.findById(recipeId).orElseGet(() -> new RecipeNutrition(recipeId));
        nutrition.update(estimate, hash(lines));
        return Optional.of(response(recipe, nutritionRepository.save(nutrition)));
    }

    // After a save: whether there is no estimate yet, or the ingredients changed since the last one
    @Transactional(readOnly = true)
    public boolean needsEstimate(UUID recipeId) {
        if (!estimator.isEnabled()) return false;
        return recipeRepository.findWithDetailsById(recipeId)
                .map(recipe -> nutritionRepository.findById(recipeId)
                        .map(nutrition -> !nutrition.getSourceHash().equals(hash(ingredientLines(recipe))))
                        .orElse(true))
                .orElse(false);
    }

    private NutritionResponse response(Recipe recipe, RecipeNutrition nutrition) {
        if (nutrition == null) {
            return new NutritionResponse(null, null, false, estimator.isEnabled());
        }
        boolean outdated = !nutrition.getSourceHash().equals(hash(ingredientLines(recipe)));
        return new NutritionResponse(nutrition.getEstimate(), nutrition.getEstimatedAt(), outdated,
                estimator.isEnabled());
    }

    // "25 dkg liszt", with a "Group:" line where a new group starts
    static List<String> ingredientLines(Recipe recipe) {
        List<String> lines = new ArrayList<>();
        String group = null;
        for (RecipeIngredient line : recipe.getIngredients()) {
            if (line.getGroup() != null && !Objects.equals(line.getGroup(), group)) {
                lines.add(line.getGroup() + ":");
            }
            group = line.getGroup();
            StringBuilder text = new StringBuilder();
            if (line.getAmount() != null) text.append(plain(line.getAmount())).append(' ');
            if (line.getUnit() != null && !line.getUnit().isBlank()) text.append(line.getUnit().strip()).append(' ');
            lines.add(text.append(line.getIngredient().getName()).toString());
        }
        return lines;
    }

    private static String plain(BigDecimal amount) {
        return amount.stripTrailingZeros().toPlainString();
    }

    static String hash(List<String> lines) {
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256")
                    .digest(String.join("\n", lines).getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(digest);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException(e);
        }
    }
}
