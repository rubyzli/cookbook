package family.cookbook.category;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
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

    public List<Category> getAllCategories(){
        return categoryRepository.findAll();
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

    public void deleteCategory(UUID id) {
        categoryRepository.deleteById(id);
    }
}
