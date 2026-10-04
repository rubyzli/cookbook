package family.cookbook.translation.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record NameTranslationRequest(@NotBlank @Size(max = 255) String name) {
}
