package family.cookbook.category.dto;

import jakarta.validation.constraints.NotBlank;

public record CreateCategoryRequest(@NotBlank String name) {
}
