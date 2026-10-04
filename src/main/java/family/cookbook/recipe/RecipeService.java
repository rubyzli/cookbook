package family.cookbook.recipe;

import family.cookbook.category.Category;
import family.cookbook.category.CategoryRepository;
import family.cookbook.ingredient.Ingredient;
import family.cookbook.ingredient.IngredientRepository;
import family.cookbook.recipe.dto.RecipeDetail;
import family.cookbook.recipe.dto.RecipeIngredientRequest;
import family.cookbook.recipe.dto.RecipeRequest;
import family.cookbook.recipe.dto.RecipeSummary;
import family.cookbook.translation.Languages;
import family.cookbook.translation.Localization;
import family.cookbook.translation.TranslationLookup;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.text.Collator;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
public class RecipeService {

    private final RecipeRepository recipeRepository;
    private final CategoryRepository categoryRepository;
    private final IngredientRepository ingredientRepository;
    private final TranslationLookup translations;

    public RecipeService(RecipeRepository recipeRepository,
                         CategoryRepository categoryRepository,
                         IngredientRepository ingredientRepository,
                         TranslationLookup translations) {
        this.recipeRepository = recipeRepository;
        this.categoryRepository = categoryRepository;
        this.ingredientRepository = ingredientRepository;
        this.translations = translations;
    }

    // language: show translations into it where they exist; empty shows the originals
    @Transactional(readOnly = true)
    public List<RecipeSummary> searchRecipes(String search, UUID categoryId, Optional<String> language) {
        String name = search == null ? "" : search.strip();
        // "" matches no translation, so only original names are searched
        String translationLanguage = language.orElse("");
        List<Recipe> recipes = categoryId == null
                ? recipeRepository.search(name, translationLanguage)
                : recipeRepository.searchInCategory(name, translationLanguage, categoryId);
        Localization localization = translations.forRecipes(language, recipes.stream().map(Recipe::getId).toList());
        Collator collator = Collator.getInstance(Locale.forLanguageTag(language.orElse(Languages.DEFAULT)));
        return recipes.stream()
                .map(recipe -> RecipeSummary.from(recipe, localization))
                .sorted(Comparator.comparing(RecipeSummary::name, collator))
                .toList();
    }

    @Transactional(readOnly = true)
    public Optional<RecipeDetail> getRecipeById(UUID id, Optional<String> language) {
        return recipeRepository.findWithDetailsById(id)
                .map(recipe -> RecipeDetail.from(recipe, translations.forRecipes(language, List.of(id))));
    }

    @Transactional
    public RecipeDetail createRecipe(RecipeRequest request) {
        if (recipeRepository.existsByNameIgnoreCase(request.name())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Recipe already exists");
        }
        Recipe recipe = new Recipe(request.name());
        recipe.setLanguage(request.language() == null ? Languages.DEFAULT : request.language());
        applyRequest(recipe, request);
        // Flush so createdAt is set before it goes into the response
        return RecipeDetail.from(recipeRepository.saveAndFlush(recipe));
    }

    @Transactional
    public Optional<RecipeDetail> updateRecipe(UUID id, RecipeRequest request) {
        Optional<Recipe> existing = recipeRepository.findWithDetailsById(id);
        if (existing.isEmpty()) {
            return Optional.empty();
        }
        if (recipeRepository.existsByNameIgnoreCaseAndIdNot(request.name(), id)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Recipe already exists");
        }
        Recipe recipe = existing.get();
        applyRequest(recipe, request);
        return Optional.of(RecipeDetail.from(recipeRepository.saveAndFlush(recipe)));
    }

    public void deleteRecipe(UUID id) {
        recipeRepository.deleteById(id);
    }

    private void applyRequest(Recipe recipe, RecipeRequest request) {
        recipe.setName(request.name());
        if (request.language() != null) recipe.setLanguage(request.language());
        recipe.setDescription(request.description());
        recipe.setServings(request.servings());
        recipe.setPrepTimeMinutes(request.prepTimeMinutes());
        recipe.setCookTimeMinutes(request.cookTimeMinutes());
        recipe.setInstructions(request.instructions());
        recipe.setNotes(request.notes());
        recipe.setImageUrl(request.imageUrl());
        recipe.setSourceUrl(request.sourceUrl() == null || request.sourceUrl().isBlank() ? null : request.sourceUrl().strip());
        recipe.replaceCategories(findCategories(request.categoryIds()));
        recipe.replaceIngredients(buildIngredientLines(recipe, request.ingredients()));
    }

    private List<Category> findCategories(List<UUID> ids) {
        Set<UUID> wanted = new LinkedHashSet<>(ids);
        List<Category> found = categoryRepository.findAllById(wanted);
        rejectMissing("category", wanted, found.stream().map(Category::getId).collect(Collectors.toSet()));
        return found;
    }

    private List<RecipeIngredient> buildIngredientLines(Recipe recipe, List<RecipeIngredientRequest> lines) {
        Set<UUID> wanted = lines.stream().map(RecipeIngredientRequest::ingredientId)
                .collect(Collectors.toCollection(LinkedHashSet::new));
        Map<UUID, Ingredient> found = ingredientRepository.findAllById(wanted).stream()
                .collect(Collectors.toMap(Ingredient::getId, Function.identity()));
        rejectMissing("ingredient", wanted, found.keySet());

        List<RecipeIngredient> result = new ArrayList<>();
        for (int position = 0; position < lines.size(); position++) {
            RecipeIngredientRequest line = lines.get(position);
            result.add(new RecipeIngredient(recipe, found.get(line.ingredientId()),
                    line.amount(), line.unit(), line.group(), position));
        }
        return result;
    }

    private static void rejectMissing(String kind, Set<UUID> wanted, Set<UUID> found) {
        List<UUID> missing = wanted.stream().filter(id -> !found.contains(id)).toList();
        if (!missing.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Unknown " + kind + " ids: " + missing);
        }
    }
}
