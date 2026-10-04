package family.cookbook.recipe.dto;

import family.cookbook.recipe.Recipe;
import family.cookbook.translation.Localization;
import family.cookbook.translation.RecipeTranslation;
import family.cookbook.translation.SourceText;
import family.cookbook.translation.TranslationStatus;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

// Texts are in `language`: a translation when one exists for the requested language, otherwise the
// original (`originalLanguage`). translationOutdated: the original was edited after it was translated.
public record RecipeDetail(
        UUID id,
        String name,
        String description,
        Integer servings,
        Integer prepTimeMinutes,
        Integer cookTimeMinutes,
        String instructions,
        String notes,
        String imageUrl,
        String createdBy,
        Instant createdAt,
        List<CategoryRef> categories,
        List<RecipeIngredientResponse> ingredients,
        String language,
        String originalLanguage,
        TranslationStatus translationStatus,
        boolean translationOutdated) {

    public static RecipeDetail from(Recipe recipe) {
        return from(recipe, Localization.original());
    }

    public static RecipeDetail from(Recipe recipe, Localization localization) {
        Optional<RecipeTranslation> translation = localization.translationOf(recipe);
        Map<String, String> groups = translation.map(RecipeTranslation::getGroups).orElse(Map.of());
        return new RecipeDetail(
                recipe.getId(),
                translation.map(RecipeTranslation::getName).orElse(recipe.getName()),
                translation.map(RecipeTranslation::getDescription).orElse(recipe.getDescription()),
                recipe.getServings(),
                recipe.getPrepTimeMinutes(),
                recipe.getCookTimeMinutes(),
                translation.map(RecipeTranslation::getInstructions).orElse(recipe.getInstructions()),
                translation.map(RecipeTranslation::getNotes).orElse(recipe.getNotes()),
                recipe.getImageUrl(),
                recipe.getCreatedBy(),
                recipe.getCreatedAt(),
                CategoryRef.sortedFrom(recipe.getCategories(), localization),
                recipe.getIngredients().stream()
                        .map(line -> RecipeIngredientResponse.from(line, localization, groups))
                        .toList(),
                translation.map(RecipeTranslation::getLanguage).orElse(recipe.getLanguage()),
                recipe.getLanguage(),
                translation.map(RecipeTranslation::getStatus).orElse(null),
                translation.map(t -> !t.getSourceHash().equals(SourceText.hash(recipe))).orElse(false));
    }
}
