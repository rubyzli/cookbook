package family.cookbook.recipe;

import family.cookbook.category.Category;
import family.cookbook.category.CategoryRepository;
import family.cookbook.ingredient.Ingredient;
import family.cookbook.ingredient.IngredientRepository;
import family.cookbook.recipe.dto.RecipeDetail;
import family.cookbook.recipe.dto.RecipeIngredientRequest;
import family.cookbook.recipe.dto.RecipeRequest;
import family.cookbook.recipe.dto.RecipeSummary;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
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

    public RecipeService(RecipeRepository recipeRepository,
                         CategoryRepository categoryRepository,
                         IngredientRepository ingredientRepository) {
        this.recipeRepository = recipeRepository;
        this.categoryRepository = categoryRepository;
        this.ingredientRepository = ingredientRepository;
    }

    @Transactional(readOnly = true)
    public List<RecipeSummary> searchRecipes(String search, UUID categoryId) {
        String name = search == null ? "" : search.strip();
        List<Recipe> recipes = categoryId == null
                ? recipeRepository.findByNameContainingIgnoreCaseOrderByNameAsc(name)
                : recipeRepository.findDistinctByCategories_IdAndNameContainingIgnoreCaseOrderByNameAsc(categoryId, name);
        return recipes.stream().map(RecipeSummary::from).toList();
    }

    @Transactional(readOnly = true)
    public Optional<RecipeDetail> getRecipeById(UUID id) {
        return recipeRepository.findWithDetailsById(id).map(RecipeDetail::from);
    }

    @Transactional
    public RecipeDetail createRecipe(RecipeRequest request) {
        if (recipeRepository.existsByNameIgnoreCase(request.name())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Recipe already exists");
        }
        Recipe recipe = new Recipe(request.name());
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
        recipe.setDescription(request.description());
        recipe.setServings(request.servings());
        recipe.setPrepTimeMinutes(request.prepTimeMinutes());
        recipe.setCookTimeMinutes(request.cookTimeMinutes());
        recipe.setInstructions(request.instructions());
        recipe.setNotes(request.notes());
        recipe.setImageUrl(request.imageUrl());
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
