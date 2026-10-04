package family.cookbook.recipe.dto;

import family.cookbook.recipe.Recipe;
import family.cookbook.translation.Localization;
import family.cookbook.translation.RecipeTranslation;
import family.cookbook.translation.TranslationStatus;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

// List view: no instructions or ingredients. name and description are in `language`: a translation
// when one exists for the requested language, otherwise the original (`originalLanguage`).
public record RecipeSummary(
        UUID id,
        String name,
        String description,
        String imageUrl,
        Integer servings,
        Integer prepTimeMinutes,
        Integer cookTimeMinutes,
        List<CategoryRef> categories,
        String language,
        String originalLanguage,
        TranslationStatus translationStatus) {

    public static RecipeSummary from(Recipe recipe) {
        return from(recipe, Localization.original());
    }

    public static RecipeSummary from(Recipe recipe, Localization localization) {
        Optional<RecipeTranslation> translation = localization.translationOf(recipe);
        return new RecipeSummary(
                recipe.getId(),
                translation.map(RecipeTranslation::getName).orElse(recipe.getName()),
                translation.map(RecipeTranslation::getDescription).orElse(recipe.getDescription()),
                recipe.getImageUrl(),
                recipe.getServings(),
                recipe.getPrepTimeMinutes(),
                recipe.getCookTimeMinutes(),
                CategoryRef.sortedFrom(recipe.getCategories(), localization),
                translation.map(RecipeTranslation::getLanguage).orElse(recipe.getLanguage()),
                recipe.getLanguage(),
                translation.map(RecipeTranslation::getStatus).orElse(null));
    }
}
