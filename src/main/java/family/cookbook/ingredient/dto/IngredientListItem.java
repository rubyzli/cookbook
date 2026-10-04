package family.cookbook.ingredient.dto;

import family.cookbook.translation.TranslationLookup.TranslatedName;

import java.util.Map;
import java.util.UUID;

// name is in the requested language where a translation exists, otherwise the original. Every
// translation is included too, so clients can match a name typed in any language, and recipeCount
// lets them warn before deleting.
public record IngredientListItem(
        UUID id,
        String name,
        String originalName,
        String originalLanguage,
        long recipeCount,
        Map<String, TranslatedName> translations) {
}
