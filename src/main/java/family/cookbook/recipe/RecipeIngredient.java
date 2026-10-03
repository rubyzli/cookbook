package family.cookbook.recipe;

import family.cookbook.ingredient.Ingredient;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;

import java.math.BigDecimal;
import java.util.UUID;

@Entity
public class RecipeIngredient {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "recipe_id")
    private Recipe recipe;
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "ingredient_id")
    private Ingredient ingredient;
    @Column(precision = 10, scale = 2)
    private BigDecimal amount;
    @Column(length = 50)
    private String unit;
    @Column(nullable = false)
    private int position;

    protected RecipeIngredient() {
    }

    public RecipeIngredient(Recipe recipe, Ingredient ingredient, BigDecimal amount, String unit, int position) {
        this.recipe = recipe;
        this.ingredient = ingredient;
        this.amount = amount;
        this.unit = unit;
        this.position = position;
    }

    public UUID getId() {
        return id;
    }

    public Recipe getRecipe() {
        return recipe;
    }

    public Ingredient getIngredient() {
        return ingredient;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public String getUnit() {
        return unit;
    }

    public int getPosition() {
        return position;
    }
}
