package family.cookbook.ingredient;

import family.cookbook.ingredient.dto.IngredientListItem;
import family.cookbook.ingredient.dto.IngredientRequest;
import family.cookbook.translation.Languages;
import jakarta.validation.Valid;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/ingredients")
public class IngredientController {

    private final IngredientService ingredientService;

    public IngredientController(IngredientService ingredientService) {
        this.ingredientService = ingredientService;
    }

    // Names come in the language asked for with Accept-Language where a translation exists
    @GetMapping
    public List<IngredientListItem> getAllIngredients(@RequestHeader(value = HttpHeaders.ACCEPT_LANGUAGE, required = false)
                                       String acceptLanguage) {
        return ingredientService.getAllIngredients(Languages.fromHeader(acceptLanguage));
    }

    @GetMapping("/{id}")
    public ResponseEntity<Ingredient> getIngredientById(@PathVariable UUID id) {
        return ResponseEntity.of(ingredientService.getIngredientById(id));
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public Ingredient createIngredient(@Valid @RequestBody IngredientRequest request) {
        return ingredientService.createIngredient(request.name(), request.language());
    }

    @PutMapping("/{id}")
    public ResponseEntity<Ingredient> renameIngredient(@PathVariable UUID id, @Valid @RequestBody IngredientRequest request) {
        return ResponseEntity.of(ingredientService.renameIngredient(id, request.name()));
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteIngredient(@PathVariable UUID id) {
        ingredientService.deleteIngredient(id);
    }
}
