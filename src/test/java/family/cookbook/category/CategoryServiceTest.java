package family.cookbook.category;

import family.cookbook.category.dto.CategoryListItem;
import family.cookbook.category.dto.CategoryUsage;
import family.cookbook.translation.TranslationLookup;
import family.cookbook.translation.TranslationLookup.TranslatedName;
import family.cookbook.translation.TranslationStatus;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CategoryServiceTest {

    @Mock
    private CategoryRepository categoryRepository;

    @Mock
    private TranslationLookup translationLookup;

    @InjectMocks
    private CategoryService categoryService;

    @Test
    void getAllCategoriesReturnsOriginalNamesSortedWithRecipeCounts() {
        UUID other = UUID.randomUUID();
        UUID main = UUID.randomUUID();
        when(categoryRepository.findAllWithRecipeCount()).thenReturn(List.of(
                new CategoryUsage(other, "Other", "hu", 0), new CategoryUsage(main, "Desserts", "hu", 3)));
        when(translationLookup.allCategoryNames()).thenReturn(Map.of());

        assertThat(categoryService.getAllCategories(Optional.empty())).containsExactly(
                new CategoryListItem(main, "Desserts", "Desserts", "hu", 3, Map.of()),
                new CategoryListItem(other, "Other", "Other", "hu", 0, Map.of()));
    }

    @Test
    void getAllCategoriesShowsTheRequestedTranslationAndSortsByIt() {
        UUID translated = UUID.randomUUID();
        UUID untranslated = UUID.randomUUID();
        Map<String, TranslatedName> names = Map.of("de", new TranslatedName("Nachspeisen", TranslationStatus.MACHINE));
        when(categoryRepository.findAllWithRecipeCount()).thenReturn(List.of(
                new CategoryUsage(translated, "Desserts", "hu", 3), new CategoryUsage(untranslated, "Apfel", "hu", 1)));
        when(translationLookup.allCategoryNames()).thenReturn(Map.of(translated, names));

        assertThat(categoryService.getAllCategories(Optional.of("de"))).containsExactly(
                new CategoryListItem(untranslated, "Apfel", "Apfel", "hu", 1, Map.of()),
                new CategoryListItem(translated, "Nachspeisen", "Desserts", "hu", 3, names));
    }

    @Test
    void getAllCategoriesKeepsTheOriginalWhenItIsAlreadyInTheRequestedLanguage() {
        UUID id = UUID.randomUUID();
        when(categoryRepository.findAllWithRecipeCount()).thenReturn(List.of(new CategoryUsage(id, "Nachspeisen", "de", 1)));
        when(translationLookup.allCategoryNames()).thenReturn(
                Map.of(id, Map.of("hu", new TranslatedName("x", TranslationStatus.REVIEWED))));

        assertThat(categoryService.getAllCategories(Optional.of("de"))).extracting(CategoryListItem::name).containsExactly("Nachspeisen");
    }

    @Test
    void createCategoryStoresTheLanguageOfTheName() {
        when(categoryRepository.existsByNameIgnoreCase("Nachspeisen")).thenReturn(false);
        when(categoryRepository.save(any(Category.class))).thenAnswer(invocation -> invocation.getArgument(0));

        assertThat(categoryService.createCategory("Nachspeisen", "de").getLanguage()).isEqualTo("de");
        assertThat(categoryService.createCategory("Nachspeisen").getLanguage()).isEqualTo("hu");
    }

    @Test
    void createCategorySavesNewNameAndReturnsSaved() {
        when(categoryRepository.existsByNameIgnoreCase("Desserts")).thenReturn(false);
        when(categoryRepository.save(any(Category.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Category created = categoryService.createCategory("Desserts");

        assertThat(created.getName()).isEqualTo("Desserts");
        verify(categoryRepository).save(created);
    }

    @Test
    void createCategoryRejectsDuplicateNameWithConflict() {
        when(categoryRepository.existsByNameIgnoreCase("Desserts")).thenReturn(true);

        assertThatThrownBy(() -> categoryService.createCategory("Desserts"))
                .isInstanceOfSatisfying(ResponseStatusException.class,
                        ex -> assertThat(ex.getStatusCode()).isEqualTo(HttpStatus.CONFLICT));
        verify(categoryRepository, never()).save(any());
    }

    @Test
    void getCategoryByIdReturnsMatchFromRepository() {
        UUID id = UUID.randomUUID();
        Category category = new Category("Desserts");
        when(categoryRepository.findById(id)).thenReturn(Optional.of(category));

        assertThat(categoryService.getCategoryById(id)).contains(category);
    }

    @Test
    void getCategoryByIdReturnsEmptyWhenMissing() {
        UUID id = UUID.randomUUID();
        when(categoryRepository.findById(id)).thenReturn(Optional.empty());

        assertThat(categoryService.getCategoryById(id)).isEmpty();
    }

    @Test
    void deleteCategoryDeletesById() {
        UUID id = UUID.randomUUID();

        categoryService.deleteCategory(id);

        verify(categoryRepository).deleteById(id);
    }

    @Test
    void renameCategorySavesNewName() {
        UUID id = UUID.randomUUID();
        Category category = new Category("Desserts");
        when(categoryRepository.findById(id)).thenReturn(Optional.of(category));
        when(categoryRepository.existsByNameIgnoreCaseAndIdNot("Renamed", id)).thenReturn(false);
        when(categoryRepository.save(category)).thenReturn(category);

        assertThat(categoryService.renameCategory(id, "Renamed")).hasValueSatisfying(
                renamed -> assertThat(renamed.getName()).isEqualTo("Renamed"));
    }

    @Test
    void renameCategoryReturnsEmptyWhenMissing() {
        UUID id = UUID.randomUUID();
        when(categoryRepository.findById(id)).thenReturn(Optional.empty());

        assertThat(categoryService.renameCategory(id, "Renamed")).isEmpty();
        verify(categoryRepository, never()).save(any());
    }

    @Test
    void renameCategoryRejectsNameUsedByAnotherWithConflict() {
        UUID id = UUID.randomUUID();
        Category category = new Category("Desserts");
        when(categoryRepository.findById(id)).thenReturn(Optional.of(category));
        when(categoryRepository.existsByNameIgnoreCaseAndIdNot("Taken", id)).thenReturn(true);

        assertThatThrownBy(() -> categoryService.renameCategory(id, "Taken"))
                .isInstanceOfSatisfying(ResponseStatusException.class,
                        ex -> assertThat(ex.getStatusCode()).isEqualTo(HttpStatus.CONFLICT));
        assertThat(category.getName()).isEqualTo("Desserts");
        verify(categoryRepository, never()).save(any());
    }
}
