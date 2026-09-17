package family.cookbook.recipe;


import family.cookbook.category.Category;
import family.cookbook.recipe.dto.CreateRecipeRequest;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Optional;
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
    public Optional<Recipe> getRecipeById(@PathVariable UUID id) {
        return recipeService.getRecipeById(id);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public Recipe createRecipe(@RequestBody CreateRecipeRequest request) {
        return recipeService.createRecipe(request.title());
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteRecipe(@PathVariable UUID id) {
        recipeService.deleteRecipe(id);
    }
}
