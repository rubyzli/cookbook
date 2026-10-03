package family.cookbook.category;

import family.cookbook.category.dto.CategoryListItem;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
public class CategoryService {

    private final CategoryRepository categoryRepository;

    public CategoryService(CategoryRepository categoryRepository) {
        this.categoryRepository = categoryRepository;
    }

    public List<CategoryListItem> getAllCategories(){
        return categoryRepository.findAllWithRecipeCount();
    }

    public Category createCategory(String name){
        if(categoryRepository.existsByNameIgnoreCase(name)){
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Category already exists");
        }
        return categoryRepository.save(new Category(name));
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
