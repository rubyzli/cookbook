package family.cookbook.category;

import family.cookbook.category.dto.CategoryListItem;
import family.cookbook.category.dto.CategoryUsage;
import family.cookbook.translation.Languages;
import family.cookbook.translation.TranslationLookup;
import family.cookbook.translation.TranslationLookup.TranslatedName;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.text.Collator;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

@Service
public class CategoryService {

    private final CategoryRepository categoryRepository;
    private final TranslationLookup translations;

    public CategoryService(CategoryRepository categoryRepository, TranslationLookup translations) {
        this.categoryRepository = categoryRepository;
        this.translations = translations;
    }

    // Sorted by the name shown in that language; empty language shows the original names
    @Transactional(readOnly = true)
    public List<CategoryListItem> getAllCategories(Optional<String> language) {
        Map<UUID, Map<String, TranslatedName>> names = translations.allCategoryNames();
        Collator collator = Collator.getInstance(Locale.forLanguageTag(language.orElse(Languages.DEFAULT)));
        return categoryRepository.findAllWithRecipeCount().stream()
                .map(usage -> toListItem(usage, names.getOrDefault(usage.id(), Map.of()), language))
                .sorted(Comparator.comparing(CategoryListItem::name, collator))
                .toList();
    }

    private static CategoryListItem toListItem(CategoryUsage usage, Map<String, TranslatedName> translated,
                                          Optional<String> language) {
        String shown = language.filter(code -> !code.equals(usage.language()))
                .map(translated::get)
                .map(TranslatedName::name)
                .orElse(usage.name());
        return new CategoryListItem(usage.id(), shown, usage.name(), usage.language(), usage.recipeCount(), translated);
    }

    public Category createCategory(String name){
        return createCategory(name, Languages.DEFAULT);
    }

    public Category createCategory(String name, String language){
        if(categoryRepository.existsByNameIgnoreCase(name)){
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Category already exists");
        }
        return categoryRepository.save(new Category(name, language == null ? Languages.DEFAULT : language));
    }

    public Optional<Category> getCategoryById(UUID id) {
        return  categoryRepository.findById(id);
    }

    @Transactional
    public Optional<Category> renameCategory(UUID id, String name) {
        Optional<Category> existing = categoryRepository.findById(id);
        if (existing.isEmpty()) {
            return Optional.empty();
        }
        if (categoryRepository.existsByNameIgnoreCaseAndIdNot(name, id)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Category already exists");
        }
        Category category = existing.get();
        category.setName(name);
        return Optional.of(categoryRepository.save(category));
    }

    public void deleteCategory(UUID id) {
        categoryRepository.deleteById(id);
    }
}
