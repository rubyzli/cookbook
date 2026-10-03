package family.cookbook.recipe.dto;

import family.cookbook.category.Category;

import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;

public record CategoryRef(UUID id, String name) {

    static List<CategoryRef> sortedFrom(Collection<Category> categories) {
        return categories.stream()
                .map(category -> new CategoryRef(category.getId(), category.getName()))
                .sorted(Comparator.comparing(CategoryRef::name, String.CASE_INSENSITIVE_ORDER))
                .toList();
    }
}
