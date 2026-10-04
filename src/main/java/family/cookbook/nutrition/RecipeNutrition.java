package family.cookbook.nutrition;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;

import java.time.Instant;
import java.util.UUID;

// Estimated calories and macros of a whole recipe, kept apart from the recipe so the background
// estimate never writes over an edit made in the meantime
@Entity
public class RecipeNutrition {

    @Id
    private UUID recipeId;
    @Column(nullable = false)
    private int kcal;
    @Column(name = "protein_g", nullable = false)
    private int proteinGrams;
    @Column(name = "carbs_g", nullable = false)
    private int carbsGrams;
    @Column(name = "fat_g", nullable = false)
    private int fatGrams;
    // Hash of the ingredient lines the estimate was made from (see NutritionService.hash)
    @Column(nullable = false, length = 64)
    private String sourceHash;
    @Column(nullable = false)
    private Instant estimatedAt;

    protected RecipeNutrition() {
    }

    public RecipeNutrition(UUID recipeId) {
        this.recipeId = recipeId;
    }

    public void update(NutritionEstimate estimate, String sourceHash) {
        this.kcal = estimate.kcal();
        this.proteinGrams = estimate.proteinGrams();
        this.carbsGrams = estimate.carbsGrams();
        this.fatGrams = estimate.fatGrams();
        this.sourceHash = sourceHash;
        this.estimatedAt = Instant.now();
    }

    public UUID getRecipeId() {
        return recipeId;
    }

    public NutritionEstimate getEstimate() {
        return new NutritionEstimate(kcal, proteinGrams, carbsGrams, fatGrams);
    }

    public String getSourceHash() {
        return sourceHash;
    }

    public Instant getEstimatedAt() {
        return estimatedAt;
    }
}
