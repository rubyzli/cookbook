package family.cookbook.recipe;

import family.cookbook.recipe.dto.CategoryRef;
import family.cookbook.recipe.dto.RecipeDetail;
import family.cookbook.recipe.dto.RecipeIngredientRequest;
import family.cookbook.recipe.dto.RecipeIngredientResponse;
import family.cookbook.recipe.dto.RecipeRequest;
import family.cookbook.recipe.dto.RecipeSummary;
import family.cookbook.translation.TranslationStatus;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(RecipeController.class)
class RecipeControllerTest {

    private static final UUID CATEGORY_ID = UUID.randomUUID();
    private static final UUID INGREDIENT_ID = UUID.randomUUID();

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private RecipeService recipeService;

    @Test
    void searchRecipesReturnsSummaries() throws Exception {
        when(recipeService.searchRecipes("", null, Optional.empty())).thenReturn(List.of(summary("Lasagna")));

        mockMvc.perform(get("/api/recipes"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].name").value("Lasagna"))
                .andExpect(jsonPath("$[0].categories[0].name").value("Italian"))
                .andExpect(jsonPath("$[0].instructions").doesNotExist());
    }

    @Test
    void searchRecipesPassesSearchAndCategoryFilter() throws Exception {
        when(recipeService.searchRecipes("las", CATEGORY_ID, Optional.empty())).thenReturn(List.of(summary("Lasagna")));

        mockMvc.perform(get("/api/recipes").param("search", "las").param("categoryId", CATEGORY_ID.toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].name").value("Lasagna"));
    }

    @Test
    void searchRecipesReturns400ForMalformedCategoryId() throws Exception {
        mockMvc.perform(get("/api/recipes").param("categoryId", "not-a-uuid"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void getRecipeByIdReturnsDetail() throws Exception {
        RecipeDetail detail = detail("Lasagna");
        when(recipeService.getRecipeById(detail.id(), Optional.empty())).thenReturn(Optional.of(detail));

        mockMvc.perform(get("/api/recipes/{id}", detail.id()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(detail.id().toString()))
                .andExpect(jsonPath("$.instructions").value("Layer and bake."))
                .andExpect(jsonPath("$.categories[0].id").value(CATEGORY_ID.toString()))
                .andExpect(jsonPath("$.ingredients[0].ingredientId").value(INGREDIENT_ID.toString()))
                .andExpect(jsonPath("$.ingredients[0].amount").value(500))
                .andExpect(jsonPath("$.ingredients[0].unit").value("g"))
                .andExpect(jsonPath("$.ingredients[0].group").value("For the layers"))
                .andExpect(jsonPath("$.notes").value("Rest 10 minutes before cutting."))
                .andExpect(jsonPath("$.language").value("de"))
                .andExpect(jsonPath("$.sourceUrl").value("https://example.com/lasagna"))
                .andExpect(jsonPath("$.originalLanguage").value("hu"))
                .andExpect(jsonPath("$.translationStatus").value("MACHINE"))
                .andExpect(jsonPath("$.translationOutdated").value(true))
                .andExpect(jsonPath("$.createdAt").value("2026-10-03T12:00:00Z"));
    }

    @Test
    void readsPassTheLanguageFromAcceptLanguage() throws Exception {
        RecipeDetail detail = detail("Lasagne");
        when(recipeService.searchRecipes("", null, Optional.of("hu"))).thenReturn(List.of(summary("Lasagne")));
        when(recipeService.getRecipeById(detail.id(), Optional.of("de"))).thenReturn(Optional.of(detail));

        mockMvc.perform(get("/api/recipes").header("Accept-Language", "hu-HU,hu;q=0.9"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].name").value("Lasagne"));
        mockMvc.perform(get("/api/recipes/{id}", detail.id()).header("Accept-Language", "de"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Lasagne"));
    }

    @Test
    void getRecipeByIdWithOriginalIgnoresTheLanguage() throws Exception {
        RecipeDetail detail = detail("Lasagna");
        when(recipeService.getRecipeById(detail.id(), Optional.empty())).thenReturn(Optional.of(detail));

        mockMvc.perform(get("/api/recipes/{id}", detail.id()).param("original", "true").header("Accept-Language", "de"))
                .andExpect(status().isOk());

        verify(recipeService, never()).getRecipeById(detail.id(), Optional.of("de"));
    }

    @Test
    void unsupportedAcceptLanguageShowsOriginals() throws Exception {
        when(recipeService.searchRecipes("", null, Optional.empty())).thenReturn(List.of());

        mockMvc.perform(get("/api/recipes").header("Accept-Language", "fr-FR"))
                .andExpect(status().isOk());
    }

    @Test
    void createRecipeReturns400ForASourceThatIsNotAWebAddress() throws Exception {
        mockMvc.perform(post("/api/recipes")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\": \"Lasagna\", \"sourceUrl\": \"javascript:alert(1)\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors.sourceUrl").exists());
    }

    @Test
    void createRecipeReturns400ForUnsupportedLanguage() throws Exception {
        mockMvc.perform(post("/api/recipes")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\": \"Lasagna\", \"language\": \"fr\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors.language").exists());
    }

    @Test
    void getRecipeByIdReturns404WhenMissing() throws Exception {
        when(recipeService.getRecipeById(any(), any())).thenReturn(Optional.empty());

        mockMvc.perform(get("/api/recipes/{id}", UUID.randomUUID()))
                .andExpect(status().isNotFound());
    }

    @Test
    void getRecipeByIdReturns400ForMalformedId() throws Exception {
        mockMvc.perform(get("/api/recipes/not-a-uuid"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void createRecipeReturns201AndPassesFullRequest() throws Exception {
        RecipeRequest expected = new RecipeRequest("Lasagna", "Classic", 6, 20, 60, "Layer and bake.",
                "Rest 10 minutes before cutting.", null, List.of(CATEGORY_ID),
                List.of(new RecipeIngredientRequest(INGREDIENT_ID, new BigDecimal("500"), "g", "For the layers")), "de",
                "https://example.com/lasagna");
        when(recipeService.createRecipe(expected)).thenReturn(detail("Lasagna"));

        mockMvc.perform(post("/api/recipes")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "name": "Lasagna",
                                  "description": "Classic",
                                  "servings": 6,
                                  "prepTimeMinutes": 20,
                                  "cookTimeMinutes": 60,
                                  "instructions": "Layer and bake.",
                                  "notes": "Rest 10 minutes before cutting.",
                                  "language": "de",
                                  "sourceUrl": "https://example.com/lasagna",
                                  "categoryIds": ["%s"],
                                  "ingredients": [{"ingredientId": "%s", "amount": 500, "unit": "g", "group": " For the layers "}]
                                }
                                """.formatted(CATEGORY_ID, INGREDIENT_ID)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.name").value("Lasagna"));
    }

    @Test
    void createRecipeTreatsMissingListsAsEmpty() throws Exception {
        RecipeRequest expected = new RecipeRequest("Lasagna", null, null, null, null, null, null, null, List.of(), List.of(), null, null);
        when(recipeService.createRecipe(expected)).thenReturn(detail("Lasagna"));

        mockMvc.perform(post("/api/recipes")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\": \"Lasagna\"}"))
                .andExpect(status().isCreated());
    }

    @Test
    void createRecipeReturns400WithFieldErrors() throws Exception {
        mockMvc.perform(post("/api/recipes")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "name": " ",
                                  "servings": 0,
                                  "prepTimeMinutes": -5,
                                  "ingredients": [{"amount": -1, "unit": "%s"}]
                                }
                                """.formatted("x".repeat(51))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detail").value("Validation failed"))
                .andExpect(jsonPath("$.errors.name").exists())
                .andExpect(jsonPath("$.errors.servings").exists())
                .andExpect(jsonPath("$.errors.prepTimeMinutes").exists())
                .andExpect(jsonPath("$.errors['ingredients[0].ingredientId']").exists())
                .andExpect(jsonPath("$.errors['ingredients[0].amount']").exists())
                .andExpect(jsonPath("$.errors['ingredients[0].unit']").exists());

        verify(recipeService, never()).createRecipe(any());
    }

    @Test
    void createRecipeTreatsBlankGroupAsNone() throws Exception {
        RecipeRequest expected = new RecipeRequest("Lasagna", null, null, null, null, null, null, null, List.of(),
                List.of(new RecipeIngredientRequest(INGREDIENT_ID, null, null, null)), null, null);
        when(recipeService.createRecipe(expected)).thenReturn(detail("Lasagna"));

        mockMvc.perform(post("/api/recipes")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\": \"Lasagna\", \"ingredients\": [{\"ingredientId\": \"%s\", \"group\": \"  \"}]}"
                                .formatted(INGREDIENT_ID)))
                .andExpect(status().isCreated());
    }

    @Test
    void createRecipeReturns400ForTooLongGroup() throws Exception {
        mockMvc.perform(post("/api/recipes")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\": \"Lasagna\", \"ingredients\": [{\"ingredientId\": \"%s\", \"group\": \"%s\"}]}"
                                .formatted(INGREDIENT_ID, "x".repeat(101))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors['ingredients[0].group']").exists());

        verify(recipeService, never()).createRecipe(any());
    }

    @Test
    void createRecipeReturns409WithReasonWhenNameExists() throws Exception {
        when(recipeService.createRecipe(any()))
                .thenThrow(new ResponseStatusException(HttpStatus.CONFLICT, "Recipe already exists"));

        mockMvc.perform(post("/api/recipes")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\": \"Lasagna\"}"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.detail").value("Recipe already exists"));
    }

    @Test
    void createRecipeReturns400WithReasonForUnknownIds() throws Exception {
        when(recipeService.createRecipe(any()))
                .thenThrow(new ResponseStatusException(HttpStatus.BAD_REQUEST, "Unknown category ids: [" + CATEGORY_ID + "]"));

        mockMvc.perform(post("/api/recipes")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\": \"Lasagna\", \"categoryIds\": [\"%s\"]}".formatted(CATEGORY_ID)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detail").value("Unknown category ids: [" + CATEGORY_ID + "]"));
    }

    @Test
    void createRecipeReturns409WhenUniqueConstraintFails() throws Exception {
        when(recipeService.createRecipe(any())).thenThrow(new DataIntegrityViolationException("duplicate key"));

        mockMvc.perform(post("/api/recipes")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\": \"Lasagna\"}"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.detail").value("Resource conflicts with existing data"));
    }

    @Test
    void updateRecipeReturnsUpdatedDetail() throws Exception {
        RecipeDetail detail = detail("Lasagna");
        when(recipeService.updateRecipe(eq(detail.id()), any())).thenReturn(Optional.of(detail));

        mockMvc.perform(put("/api/recipes/{id}", detail.id())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\": \"Lasagna\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Lasagna"));
    }

    @Test
    void updateRecipeReturns404WhenMissing() throws Exception {
        when(recipeService.updateRecipe(any(), any())).thenReturn(Optional.empty());

        mockMvc.perform(put("/api/recipes/{id}", UUID.randomUUID())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\": \"Lasagna\"}"))
                .andExpect(status().isNotFound());
    }

    @Test
    void updateRecipeReturns400ForBlankName() throws Exception {
        mockMvc.perform(put("/api/recipes/{id}", UUID.randomUUID())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\": \"\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors.name").exists());

        verify(recipeService, never()).updateRecipe(any(), any());
    }

    @Test
    void deleteRecipeReturns204() throws Exception {
        UUID id = UUID.randomUUID();

        mockMvc.perform(delete("/api/recipes/{id}", id))
                .andExpect(status().isNoContent());

        verify(recipeService).deleteRecipe(id);
    }

    private static RecipeSummary summary(String name) {
        return new RecipeSummary(UUID.randomUUID(), name, null, null, 6, 20, 60,
                List.of(new CategoryRef(CATEGORY_ID, "Italian")), "hu", "hu", null);
    }

    private static RecipeDetail detail(String name) {
        return new RecipeDetail(UUID.randomUUID(), name, "Classic", 6, 20, 60, "Layer and bake.",
                "Rest 10 minutes before cutting.", null, "https://example.com/lasagna", null, Instant.parse("2026-10-03T12:00:00Z"),
                List.of(new CategoryRef(CATEGORY_ID, "Italian")),
                List.of(new RecipeIngredientResponse(INGREDIENT_ID, "Pasta sheets", new BigDecimal("500"), "g",
                        "For the layers")), "de", "hu", TranslationStatus.MACHINE, true);
    }
}
