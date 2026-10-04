package family.cookbook.recipe;

import family.cookbook.category.Category;
import family.cookbook.category.CategoryRepository;
import family.cookbook.ingredient.Ingredient;
import family.cookbook.ingredient.IngredientRepository;
import family.cookbook.recipe.dto.CategoryRef;
import family.cookbook.recipe.dto.RecipeDetail;
import family.cookbook.recipe.dto.RecipeIngredientRequest;
import family.cookbook.recipe.dto.RecipeIngredientResponse;
import family.cookbook.recipe.dto.RecipeRequest;
import family.cookbook.recipe.dto.RecipeSummary;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RecipeServiceTest {

    @Mock
    private RecipeRepository recipeRepository;

    @Mock
    private CategoryRepository categoryRepository;

    @Mock
    private IngredientRepository ingredientRepository;

    @InjectMocks
    private RecipeService recipeService;

    @Test
    void searchRecipesWithoutCategorySearchesByTrimmedName() {
        Recipe recipe = recipe("Lasagna");
        when(recipeRepository.findByNameContainingIgnoreCaseOrderByNameAsc("las")).thenReturn(List.of(recipe));

        List<RecipeSummary> result = recipeService.searchRecipes("  las ", null);

        assertThat(result).extracting(RecipeSummary::name).containsExactly("Lasagna");
    }

    @Test
    void searchRecipesWithCategoryFiltersByCategory() {
        UUID categoryId = UUID.randomUUID();
        when(recipeRepository.findDistinctByCategories_IdAndNameContainingIgnoreCaseOrderByNameAsc(categoryId, ""))
                .thenReturn(List.of(recipe("Lasagna")));

        List<RecipeSummary> result = recipeService.searchRecipes("", categoryId);

        assertThat(result).extracting(RecipeSummary::name).containsExactly("Lasagna");
        verify(recipeRepository, never()).findByNameContainingIgnoreCaseOrderByNameAsc(any());
    }

    @Test
    void searchRecipesTreatsNullSearchAsMatchAll() {
        when(recipeRepository.findByNameContainingIgnoreCaseOrderByNameAsc("")).thenReturn(List.of());

        assertThat(recipeService.searchRecipes(null, null)).isEmpty();
    }

    @Test
    void getRecipeByIdReturnsDetail() {
        Recipe recipe = recipe("Lasagna");
        recipe.setInstructions("Layer and bake.");
        when(recipeRepository.findWithDetailsById(recipe.getId())).thenReturn(Optional.of(recipe));

        assertThat(recipeService.getRecipeById(recipe.getId()))
                .hasValueSatisfying(detail -> {
                    assertThat(detail.id()).isEqualTo(recipe.getId());
                    assertThat(detail.instructions()).isEqualTo("Layer and bake.");
                });
    }

    @Test
    void getRecipeByIdReturnsEmptyWhenMissing() {
        UUID id = UUID.randomUUID();
        when(recipeRepository.findWithDetailsById(id)).thenReturn(Optional.empty());

        assertThat(recipeService.getRecipeById(id)).isEmpty();
    }

    @Test
    void createRecipeAppliesFieldsCategoriesAndOrderedIngredientLines() {
        Category dessert = category("Dessert");
        Category baking = category("Baking");
        Ingredient flour = ingredient("Flour");
        Ingredient butter = ingredient("Butter");
        RecipeRequest request = new RecipeRequest("Apple Pie", "Grandma's", 8, 30, 45, "Mix. Bake.",
                "Use tart apples.", null,
                List.of(dessert.getId(), baking.getId()),
                List.of(line(flour, "250", "g", "For the dough"), line(butter, "125", "g", "For the dough"),
                        line(butter, "1", "tbsp", "For the top")));
        when(recipeRepository.existsByNameIgnoreCase("Apple Pie")).thenReturn(false);
        when(categoryRepository.findAllById(Set.of(dessert.getId(), baking.getId()))).thenReturn(List.of(dessert, baking));
        when(ingredientRepository.findAllById(Set.of(flour.getId(), butter.getId()))).thenReturn(List.of(flour, butter));
        when(recipeRepository.saveAndFlush(any(Recipe.class))).thenAnswer(invocation -> invocation.getArgument(0));

        RecipeDetail created = recipeService.createRecipe(request);

        assertThat(created.name()).isEqualTo("Apple Pie");
        assertThat(created.description()).isEqualTo("Grandma's");
        assertThat(created.servings()).isEqualTo(8);
        assertThat(created.prepTimeMinutes()).isEqualTo(30);
        assertThat(created.cookTimeMinutes()).isEqualTo(45);
        assertThat(created.instructions()).isEqualTo("Mix. Bake.");
        assertThat(created.notes()).isEqualTo("Use tart apples.");
        assertThat(created.categories()).extracting(CategoryRef::name).containsExactly("Baking", "Dessert");
        assertThat(created.ingredients()).containsExactly(
                new RecipeIngredientResponse(flour.getId(), "Flour", new BigDecimal("250"), "g", "For the dough"),
                new RecipeIngredientResponse(butter.getId(), "Butter", new BigDecimal("125"), "g", "For the dough"),
                new RecipeIngredientResponse(butter.getId(), "Butter", new BigDecimal("1"), "tbsp", "For the top"));
    }

    @Test
    void createRecipeNumbersIngredientLinesByPosition() {
        Ingredient flour = ingredient("Flour");
        Ingredient butter = ingredient("Butter");
        RecipeRequest request = request("Shortbread", List.of(), List.of(line(butter, "1", "cup"), line(flour, "2", "cups")));
        when(ingredientRepository.findAllById(any())).thenReturn(List.of(flour, butter));
        when(recipeRepository.saveAndFlush(any(Recipe.class))).thenAnswer(invocation -> {
            Recipe saved = invocation.getArgument(0);
            assertThat(saved.getIngredients()).extracting(RecipeIngredient::getPosition).containsExactly(0, 1);
            assertThat(saved.getIngredients()).extracting(RecipeIngredient::getRecipe).containsOnly(saved);
            return saved;
        });

        recipeService.createRecipe(request);
    }

    @Test
    void createRecipeRejectsDuplicateNameWithConflict() {
        when(recipeRepository.existsByNameIgnoreCase("Lasagna")).thenReturn(true);

        assertThatThrownBy(() -> recipeService.createRecipe(request("Lasagna", List.of(), List.of())))
                .isInstanceOfSatisfying(ResponseStatusException.class,
                        ex -> assertThat(ex.getStatusCode()).isEqualTo(HttpStatus.CONFLICT));
        verify(recipeRepository, never()).saveAndFlush(any());
    }

    @Test
    void createRecipeRejectsUnknownCategoryWithBadRequest() {
        Category known = category("Dessert");
        UUID unknown = UUID.randomUUID();
        when(categoryRepository.findAllById(anyCollection())).thenReturn(List.of(known));

        assertThatThrownBy(() -> recipeService.createRecipe(request("Pie", List.of(known.getId(), unknown), List.of())))
                .isInstanceOfSatisfying(ResponseStatusException.class, ex -> {
                    assertThat(ex.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
                    assertThat(ex.getReason()).contains(unknown.toString()).doesNotContain(known.getId().toString());
                });
        verify(recipeRepository, never()).saveAndFlush(any());
    }

    @Test
    void createRecipeRejectsUnknownIngredientWithBadRequest() {
        UUID unknown = UUID.randomUUID();
        when(ingredientRepository.findAllById(anyCollection())).thenReturn(List.of());

        RecipeRequest request = request("Pie", List.of(),
                List.of(new RecipeIngredientRequest(unknown, null, null, null)));

        assertThatThrownBy(() -> recipeService.createRecipe(request))
                .isInstanceOfSatisfying(ResponseStatusException.class, ex -> {
                    assertThat(ex.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
                    assertThat(ex.getReason()).contains("ingredient").contains(unknown.toString());
                });
        verify(recipeRepository, never()).saveAndFlush(any());
    }

    @Test
    void updateRecipeReplacesFieldsCategoriesAndIngredients() {
        Category dessert = category("Dessert");
        Category italian = category("Italian");
        Ingredient flour = ingredient("Flour");
        Ingredient sugar = ingredient("Sugar");
        Recipe recipe = recipe("Pie");
        recipe.setServings(4);
        recipe.replaceCategories(List.of(dessert));
        recipe.replaceIngredients(List.of(new RecipeIngredient(recipe, flour, BigDecimal.ONE, "cup", "Old group", 0)));
        when(recipeRepository.findWithDetailsById(recipe.getId())).thenReturn(Optional.of(recipe));
        when(recipeRepository.existsByNameIgnoreCaseAndIdNot("Apple Pie", recipe.getId())).thenReturn(false);
        when(categoryRepository.findAllById(Set.of(italian.getId()))).thenReturn(List.of(italian));
        when(ingredientRepository.findAllById(Set.of(sugar.getId()))).thenReturn(List.of(sugar));
        when(recipeRepository.saveAndFlush(recipe)).thenReturn(recipe);

        RecipeRequest request = new RecipeRequest("Apple Pie", null, null, null, null, null, null, null,
                List.of(italian.getId()), List.of(line(sugar, "100", "g")));
        Optional<RecipeDetail> updated = recipeService.updateRecipe(recipe.getId(), request);

        assertThat(updated).hasValueSatisfying(detail -> {
            assertThat(detail.name()).isEqualTo("Apple Pie");
            assertThat(detail.servings()).isNull();
            assertThat(detail.categories()).extracting(CategoryRef::name).containsExactly("Italian");
            assertThat(detail.ingredients()).extracting(RecipeIngredientResponse::name).containsExactly("Sugar");
            assertThat(detail.ingredients()).extracting(RecipeIngredientResponse::group).containsExactly((String) null);
        });
    }

    @Test
    void updateRecipeReturnsEmptyWhenMissing() {
        UUID id = UUID.randomUUID();
        when(recipeRepository.findWithDetailsById(id)).thenReturn(Optional.empty());

        assertThat(recipeService.updateRecipe(id, request("Pie", List.of(), List.of()))).isEmpty();
        verify(recipeRepository, never()).saveAndFlush(any());
    }

    @Test
    void updateRecipeRejectsNameUsedByAnotherRecipe() {
        Recipe recipe = recipe("Pie");
        when(recipeRepository.findWithDetailsById(recipe.getId())).thenReturn(Optional.of(recipe));
        when(recipeRepository.existsByNameIgnoreCaseAndIdNot("Lasagna", recipe.getId())).thenReturn(true);

        assertThatThrownBy(() -> recipeService.updateRecipe(recipe.getId(), request("Lasagna", List.of(), List.of())))
                .isInstanceOfSatisfying(ResponseStatusException.class,
                        ex -> assertThat(ex.getStatusCode()).isEqualTo(HttpStatus.CONFLICT));
        assertThat(recipe.getName()).isEqualTo("Pie");
        verify(recipeRepository, never()).saveAndFlush(any());
    }

    @Test
    void deleteRecipeDeletesById() {
        UUID id = UUID.randomUUID();

        recipeService.deleteRecipe(id);

        verify(recipeRepository).deleteById(id);
    }

    private static RecipeRequest request(String name, List<UUID> categoryIds, List<RecipeIngredientRequest> ingredients) {
        return new RecipeRequest(name, null, null, null, null, null, null, null, categoryIds, ingredients);
    }

    private static RecipeIngredientRequest line(Ingredient ingredient, String amount, String unit) {
        return line(ingredient, amount, unit, null);
    }

    private static RecipeIngredientRequest line(Ingredient ingredient, String amount, String unit, String group) {
        return new RecipeIngredientRequest(ingredient.getId(), new BigDecimal(amount), unit, group);
    }

    private static Recipe recipe(String name) {
        Recipe recipe = new Recipe(name);
        recipe.setId(UUID.randomUUID());
        return recipe;
    }

    private static Category category(String name) {
        Category category = new Category(name);
        category.setId(UUID.randomUUID());
        return category;
    }

    private static Ingredient ingredient(String name) {
        Ingredient ingredient = new Ingredient(name);
        ingredient.setId(UUID.randomUUID());
        return ingredient;
    }
}
