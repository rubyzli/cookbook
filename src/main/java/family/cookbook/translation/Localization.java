package family.cookbook.translation;

import family.cookbook.category.Category;
import family.cookbook.ingredient.Ingredient;
import family.cookbook.recipe.Recipe;

import java.util.Map;
import java.util.Optional;
import java.util.UUID;

// Which text to show for one requested language: a translation where there is one, otherwise the
// original. A null language means "originals only", as the edit form needs.
public final class Localization {

    private static final Localization ORIGINAL = new Localization(null, Map.of(), Map.of(), Map.of());

    private final String language;
    private final Map<UUID, RecipeTranslation> recipes;
    private final Map<UUID, String> categories;
    private final Map<UUID, String> ingredients;

    public Localization(String language, Map<UUID, RecipeTranslation> recipes,
                        Map<UUID, String> categories, Map<UUID, String> ingredients) {
        this.language = language;
        this.recipes = recipes;
        this.categories = categories;
        this.ingredients = ingredients;
    }

    public static Localization original() {
        return ORIGINAL;
    }

    public Optional<RecipeTranslation> translationOf(Recipe recipe) {
        if (language == null || language.equals(recipe.getLanguage())) return Optional.empty();
        return Optional.ofNullable(recipes.get(recipe.getId()));
    }

    public String nameOf(Category category) {
        if (language == null || language.equals(category.getLanguage())) return category.getName();
        return categories.getOrDefault(category.getId(), category.getName());
    }

    public String nameOf(Ingredient ingredient) {
        if (language == null || language.equals(ingredient.getLanguage())) return ingredient.getName();
        return ingredients.getOrDefault(ingredient.getId(), ingredient.getName());
    }
}
