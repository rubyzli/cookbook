package family.cookbook.category;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
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

    @InjectMocks
    private CategoryService categoryService;

    @Test
    void getAllCategoriesReturnsEverythingFromRepository() {
        List<Category> categories = List.of(new Category("Desserts"), new Category("Other"));
        when(categoryRepository.findAll()).thenReturn(categories);

        assertThat(categoryService.getAllCategories()).isEqualTo(categories);
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
}
