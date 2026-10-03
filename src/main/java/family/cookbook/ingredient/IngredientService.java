package family.cookbook.ingredient;

import family.cookbook.ingredient.dto.IngredientListItem;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
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

    public List<IngredientListItem> getAllIngredients(){
        return ingredientRepository.findAllWithRecipeCount();
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

    @Transactional
    public Optional<Ingredient> renameIngredient(UUID id, String name) {
        Optional<Ingredient> existing = ingredientRepository.findById(id);
        if (existing.isEmpty()) {
            return Optional.empty();
        }
        if (ingredientRepository.existsByNameIgnoreCaseAndIdNot(name, id)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Ingredient already exists");
        }
        Ingredient ingredient = existing.get();
        ingredient.setName(name);
        return Optional.of(ingredientRepository.save(ingredient));
    }

    public void deleteIngredient(UUID id) {
        // The foreign key would reject this anyway; checking first gives a clear message
        long recipeCount = ingredientRepository.countRecipesUsing(id);
        if (recipeCount > 0) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "Ingredient is used by " + recipeCount + (recipeCount == 1 ? " recipe" : " recipes"));
        }
        ingredientRepository.deleteById(id);
    }
}
