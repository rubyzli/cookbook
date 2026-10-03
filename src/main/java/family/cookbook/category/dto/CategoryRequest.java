package family.cookbook.category.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

// Used for both create and rename
public record CategoryRequest(@NotBlank @Size(max = 255) String name) {
}
