package family.cookbook.translation;

import family.cookbook.translation.dto.MachineFillResult;
import family.cookbook.translation.dto.NameTranslationRequest;
import family.cookbook.translation.dto.RecipeTranslationRequest;
import family.cookbook.translation.dto.RecipeTranslationResponse;
import family.cookbook.translation.dto.RecipeTranslations;
import family.cookbook.translation.dto.TranslationSettings;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.TreeSet;
import java.util.UUID;

@RestController
@RequestMapping("/api")
public class TranslationController {

    private final TranslationService translationService;

    public TranslationController(TranslationService translationService) {
        this.translationService = translationService;
    }

    @GetMapping("/translations/settings")
    public TranslationSettings settings() {
        return new TranslationSettings(translationService.machineTranslationEnabled(),
                List.copyOf(new TreeSet<>(Languages.SUPPORTED)));
    }

    // Recipes

    @GetMapping("/recipes/{id}/translations")
    public ResponseEntity<RecipeTranslations> getTranslations(@PathVariable UUID id) {
        return ResponseEntity.of(translationService.getTranslations(id));
    }

    @PostMapping("/recipes/{id}/translations/{language}/machine")
    public ResponseEntity<RecipeTranslationResponse> machineTranslate(@PathVariable UUID id,
                                                                      @PathVariable String language) {
        return ResponseEntity.of(translationService.machineTranslate(id, Languages.require(language)));
    }

    @PutMapping("/recipes/{id}/translations/{language}")
    public ResponseEntity<RecipeTranslationResponse> saveTranslation(@PathVariable UUID id,
                                                                     @PathVariable String language,
                                                                     @Valid @RequestBody RecipeTranslationRequest request) {
        return ResponseEntity.of(translationService.saveTranslation(id, Languages.require(language), request));
    }

    @DeleteMapping("/recipes/{id}/translations/{language}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteTranslation(@PathVariable UUID id, @PathVariable String language) {
        translationService.deleteTranslation(id, Languages.require(language));
    }

    // Ingredient and category names

    @PutMapping("/ingredients/{id}/translations/{language}")
    public ResponseEntity<Void> setIngredientName(@PathVariable UUID id, @PathVariable String language,
                                                  @Valid @RequestBody NameTranslationRequest request) {
        return found(translationService.setIngredientName(id, Languages.require(language), request.name()));
    }

    @DeleteMapping("/ingredients/{id}/translations/{language}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteIngredientName(@PathVariable UUID id, @PathVariable String language) {
        translationService.deleteIngredientName(id, Languages.require(language));
    }

    @PostMapping("/ingredients/translations/{language}/machine")
    public MachineFillResult translateIngredientNames(@PathVariable String language) {
        return translationService.translateMissingIngredientNames(Languages.require(language));
    }

    @PutMapping("/categories/{id}/translations/{language}")
    public ResponseEntity<Void> setCategoryName(@PathVariable UUID id, @PathVariable String language,
                                                @Valid @RequestBody NameTranslationRequest request) {
        return found(translationService.setCategoryName(id, Languages.require(language), request.name()));
    }

    @DeleteMapping("/categories/{id}/translations/{language}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteCategoryName(@PathVariable UUID id, @PathVariable String language) {
        translationService.deleteCategoryName(id, Languages.require(language));
    }

    @PostMapping("/categories/translations/{language}/machine")
    public MachineFillResult translateCategoryNames(@PathVariable String language) {
        return translationService.translateMissingCategoryNames(Languages.require(language));
    }

    private static ResponseEntity<Void> found(boolean found) {
        return found ? ResponseEntity.noContent().build() : ResponseEntity.notFound().build();
    }
}
