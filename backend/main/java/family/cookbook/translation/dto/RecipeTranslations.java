package family.cookbook.translation.dto;

import java.util.List;

// Everything the translation page needs: the original text and all existing translations
public record RecipeTranslations(
        String originalLanguage,
        Original original,
        List<RecipeTranslationResponse> translations) {

    public record Original(String name, String description, String instructions, String notes, List<String> groups) {
    }
}
