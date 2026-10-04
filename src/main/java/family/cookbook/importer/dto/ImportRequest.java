package family.cookbook.importer.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record ImportRequest(@NotBlank @Size(max = 1000) String url) {
}
