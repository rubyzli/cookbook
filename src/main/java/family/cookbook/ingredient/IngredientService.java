package family.cookbook.ingredient;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
public class IngredientService {

    private final IngredientRepository ingredientRepository;

    public IngredientService(IngredientRepository ingredientRepository) {
        this.ingredientRepository = ingredientRepository;
    }

    public List<Ingredient> getAllIngredients(){
        return ingredientRepository.findAll();
    }

    public Ingredient createIngredient(String name){
        if(ingredientRepository.existsByNameIgnoreCase(name)){
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Ingredient already exists");
        }
        return ingredientRepository.save(new Ingredient(name));
    }

    public Optional<Ingredient> getIngredientById(UUID id) {
        return  ingredientRepository.findById(id);
    }

    public void deleteIngredient(UUID id) {
        ingredientRepository.deleteById(id);
    }
}
