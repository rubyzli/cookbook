package family.cookbook.translation.dto;

import family.cookbook.translation.TranslationStatus;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.util.Map;

// A translation written or corrected by a person. groups maps original group headings to translated
// ones. status defaults to REVIEWED; MACHINE keeps the "machine-translated" mark.
public record RecipeTranslationRequest(
        @NotBlank @Size(max = 255) String name,
        @Size(max = 255) String description,
        String instructions,
        String notes,
        Map<@Size(max = 100) String, @NotBlank @Size(max = 100) String> groups,
        TranslationStatus status) {

    public RecipeTranslationRequest {
        groups = groups == null ? Map.of() : groups;
        status = status == null ? TranslationStatus.REVIEWED : status;
    }
}
