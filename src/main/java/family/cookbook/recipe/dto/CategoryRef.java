package family.cookbook.recipe.dto;

import family.cookbook.category.Category;
import family.cookbook.translation.Localization;

import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;

public record CategoryRef(UUID id, String name) {

    static List<CategoryRef> sortedFrom(Collection<Category> categories, Localization localization) {
        return categories.stream()
                .map(category -> new CategoryRef(category.getId(), localization.nameOf(category)))
                .sorted(Comparator.comparing(CategoryRef::name, String.CASE_INSENSITIVE_ORDER))
                .toList();
    }
}
