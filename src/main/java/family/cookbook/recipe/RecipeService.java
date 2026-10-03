package family.cookbook.recipe;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
public class RecipeService {

    private final RecipeRepository recipeRepository;

    public RecipeService(RecipeRepository recipeRepository) {
        this.recipeRepository = recipeRepository;
    }

    public List<Recipe> getAllRecipes() {
        return recipeRepository.findAll();
    }

    public Recipe createRecipe(String name){
        if(recipeRepository.existsByNameIgnoreCase(name)){
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Recipe already exists");
        }
        return recipeRepository.save(new Recipe(name));
    }

    public void deleteRecipe(UUID id) {
        recipeRepository.deleteById(id);
    }

    public Optional<Recipe> getRecipeById(UUID id) {
        return recipeRepository.findById(id);
    }
}
