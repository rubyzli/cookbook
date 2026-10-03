package family.cookbook.recipe;


import family.cookbook.recipe.dto.RecipeDetail;
import family.cookbook.recipe.dto.RecipeRequest;
import family.cookbook.recipe.dto.RecipeSummary;
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
    public List<RecipeSummary> searchRecipes(@RequestParam(defaultValue = "") String search,
                                             @RequestParam(required = false) UUID categoryId) {
        return recipeService.searchRecipes(search, categoryId);
    }

    @GetMapping("/{id}")
    public ResponseEntity<RecipeDetail> getRecipeById(@PathVariable UUID id) {
        return ResponseEntity.of(recipeService.getRecipeById(id));
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public RecipeDetail createRecipe(@Valid @RequestBody RecipeRequest request) {
        return recipeService.createRecipe(request);
    }

    @PutMapping("/{id}")
    public ResponseEntity<RecipeDetail> updateRecipe(@PathVariable UUID id, @Valid @RequestBody RecipeRequest request) {
        return ResponseEntity.of(recipeService.updateRecipe(id, request));
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteRecipe(@PathVariable UUID id) {
        recipeService.deleteRecipe(id);
    }
}
