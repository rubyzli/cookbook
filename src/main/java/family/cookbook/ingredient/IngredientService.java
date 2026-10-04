package family.cookbook.ingredient;

import family.cookbook.ingredient.dto.IngredientListItem;
import family.cookbook.ingredient.dto.IngredientUsage;
import family.cookbook.translation.Languages;
import family.cookbook.translation.TranslationLookup;
import family.cookbook.translation.TranslationLookup.TranslatedName;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.text.Collator;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

@Service
public class IngredientService {

    private final IngredientRepository ingredientRepository;
    private final TranslationLookup translations;

    public IngredientService(IngredientRepository ingredientRepository, TranslationLookup translations) {
        this.ingredientRepository = ingredientRepository;
        this.translations = translations;
    }

    // Sorted by the name shown in that language; empty language shows the original names
    @Transactional(readOnly = true)
    public List<IngredientListItem> getAllIngredients(Optional<String> language) {
        Map<UUID, Map<String, TranslatedName>> names = translations.allIngredientNames();
        Collator collator = Collator.getInstance(Locale.forLanguageTag(language.orElse(Languages.DEFAULT)));
        return ingredientRepository.findAllWithRecipeCount().stream()
                .map(usage -> toListItem(usage, names.getOrDefault(usage.id(), Map.of()), language))
                .sorted(Comparator.comparing(IngredientListItem::name, collator))
                .toList();
    }

    private static IngredientListItem toListItem(IngredientUsage usage, Map<String, TranslatedName> translated,
                                          Optional<String> language) {
        String shown = language.filter(code -> !code.equals(usage.language()))
                .map(translated::get)
                .map(TranslatedName::name)
                .orElse(usage.name());
        return new IngredientListItem(usage.id(), shown, usage.name(), usage.language(), usage.recipeCount(), translated);
    }

    public Ingredient createIngredient(String name){
        return createIngredient(name, Languages.DEFAULT);
    }

    public Ingredient createIngredient(String name, String language){
        if(ingredientRepository.existsByNameIgnoreCase(name)){
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Ingredient already exists");
        }
        return ingredientRepository.save(new Ingredient(name, language == null ? Languages.DEFAULT : language));
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
