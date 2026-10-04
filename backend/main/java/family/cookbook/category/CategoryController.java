package family.cookbook.category;

import family.cookbook.category.dto.CategoryListItem;
import family.cookbook.category.dto.CategoryRequest;
import family.cookbook.translation.Languages;
import jakarta.validation.Valid;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/categories")
public class CategoryController {

    private final CategoryService categoryService;

    public CategoryController(CategoryService categoryService) {
        this.categoryService = categoryService;
    }

    // Names come in the language asked for with Accept-Language where a translation exists
    @GetMapping
    public List<CategoryListItem> getAllCategories(@RequestHeader(value = HttpHeaders.ACCEPT_LANGUAGE, required = false)
                                       String acceptLanguage) {
        return categoryService.getAllCategories(Languages.fromHeader(acceptLanguage));
    }

    @GetMapping("/{id}")
    public ResponseEntity<Category> getCategoryById(@PathVariable UUID id) {
        return ResponseEntity.of(categoryService.getCategoryById(id));
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public Category createCategory(@Valid @RequestBody CategoryRequest request) {
        return categoryService.createCategory(request.name(), request.language());
    }

    @PutMapping("/{id}")
    public ResponseEntity<Category> renameCategory(@PathVariable UUID id, @Valid @RequestBody CategoryRequest request) {
        return ResponseEntity.of(categoryService.renameCategory(id, request.name()));
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteCategory(@PathVariable UUID id) {
        categoryService.deleteCategory(id);
    }
}
