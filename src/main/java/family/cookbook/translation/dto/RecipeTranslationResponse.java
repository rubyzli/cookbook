package family.cookbook.translation.dto;

import family.cookbook.translation.RecipeTranslation;
import family.cookbook.translation.TranslationStatus;

import java.time.Instant;
import java.util.Map;

// outdated: the original was edited after this translation was made
public record RecipeTranslationResponse(
        String language,
        TranslationStatus status,
        boolean outdated,
        String name,
        String description,
        String instructions,
        String notes,
        Map<String, String> groups,
        Instant updatedAt) {

    public static RecipeTranslationResponse from(RecipeTranslation translation, String currentSourceHash) {
        return new RecipeTranslationResponse(
                translation.getLanguage(),
                translation.getStatus(),
                !translation.getSourceHash().equals(currentSourceHash),
                translation.getName(),
                translation.getDescription(),
                translation.getInstructions(),
                translation.getNotes(),
                Map.copyOf(translation.getGroups()),
                translation.getUpdatedAt());
    }
}
