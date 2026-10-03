package family.cookbook.recipe;


import family.cookbook.recipe.dto.CreateRecipeRequest;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/recipes")
public class RecipeController {

    private final RecipeService recipeService;

    public RecipeController(RecipeService recipeService) {
        this.recipeService = recipeService;
    }

    @GetMapping
    public List<Recipe> getAllRecipes() {
        return recipeService.getAllRecipes();
    }

    @GetMapping("/{id}")
    public ResponseEntity<Recipe> getRecipeById(@PathVariable UUID id) {
        return ResponseEntity.of(recipeService.getRecipeById(id));
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public Recipe createRecipe(@Valid @RequestBody CreateRecipeRequest request) {
        return recipeService.createRecipe(request.name());
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteRecipe(@PathVariable UUID id) {
        recipeService.deleteRecipe(id);
    }
}
