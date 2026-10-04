package family.cookbook.translation;

import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

// Loads the translations a page needs in a few queries, instead of one per recipe or ingredient
@Component
public class TranslationLookup {

    private final RecipeTranslationRepository recipeTranslations;
    private final CategoryTranslationRepository categoryTranslations;
    private final IngredientTranslationRepository ingredientTranslations;

    public TranslationLookup(RecipeTranslationRepository recipeTranslations,
                             CategoryTranslationRepository categoryTranslations,
                             IngredientTranslationRepository ingredientTranslations) {
        this.recipeTranslations = recipeTranslations;
        this.categoryTranslations = categoryTranslations;
        this.ingredientTranslations = ingredientTranslations;
    }

    @Transactional(readOnly = true)
    public Localization forRecipes(Optional<String> language, Collection<UUID> recipeIds) {
        if (language.isEmpty()) return Localization.original();
        String code = language.get();
        Map<UUID, RecipeTranslation> recipes = recipeIds.isEmpty() ? Map.of()
                : recipeTranslations.findByIdLanguageAndIdOwnerIdIn(code, recipeIds).stream()
                        .collect(Collectors.toMap(RecipeTranslation::getRecipeId, Function.identity()));
        return new Localization(code, recipes, categoryNames(code), ingredientNames(code));
    }

    @Transactional(readOnly = true)
    public Map<UUID, String> categoryNames(String language) {
        return categoryTranslations.findByIdLanguage(language).stream()
                .collect(Collectors.toMap(CategoryTranslation::getCategoryId, CategoryTranslation::getName));
    }

    @Transactional(readOnly = true)
    public Map<UUID, String> ingredientNames(String language) {
        return ingredientTranslations.findByIdLanguage(language).stream()
                .collect(Collectors.toMap(IngredientTranslation::getIngredientId, IngredientTranslation::getName));
    }

    // Every translated name, by owner id and then language, for the category and ingredient lists
    @Transactional(readOnly = true)
    public Map<UUID, Map<String, TranslatedName>> allCategoryNames() {
        return group(categoryTranslations.findAll(), CategoryTranslation::getCategoryId, CategoryTranslation::getLanguage,
                t -> new TranslatedName(t.getName(), t.getStatus()));
    }

    @Transactional(readOnly = true)
    public Map<UUID, Map<String, TranslatedName>> allIngredientNames() {
        return group(ingredientTranslations.findAll(), IngredientTranslation::getIngredientId,
                IngredientTranslation::getLanguage, t -> new TranslatedName(t.getName(), t.getStatus()));
    }

    private static <T> Map<UUID, Map<String, TranslatedName>> group(List<T> rows, Function<T, UUID> owner,
                                                                    Function<T, String> language,
                                                                    Function<T, TranslatedName> name) {
        return rows.stream().collect(Collectors.groupingBy(owner, Collectors.toMap(language, name)));
    }

    public record TranslatedName(String name, TranslationStatus status) {
    }
}
