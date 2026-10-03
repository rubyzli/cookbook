package family.cookbook.recipe;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(RecipeController.class)
class RecipeControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private RecipeService recipeService;

    @Test
    void getAllRecipesReturnsList() throws Exception {
        when(recipeService.getAllRecipes()).thenReturn(List.of(recipe("Lasagna")));

        mockMvc.perform(get("/api/recipes"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].name").value("Lasagna"));
    }

    @Test
    void getRecipeByIdReturnsMatch() throws Exception {
        Recipe recipe = recipe("Lasagna");
        when(recipeService.getRecipeById(recipe.getId())).thenReturn(Optional.of(recipe));

        mockMvc.perform(get("/api/recipes/{id}", recipe.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(recipe.getId().toString()))
                .andExpect(jsonPath("$.name").value("Lasagna"));
    }

    @Test
    void getRecipeByIdReturns404WhenMissing() throws Exception {
        when(recipeService.getRecipeById(any())).thenReturn(Optional.empty());

        mockMvc.perform(get("/api/recipes/{id}", UUID.randomUUID()))
                .andExpect(status().isNotFound());
    }

    @Test
    void getRecipeByIdReturns400ForMalformedId() throws Exception {
        mockMvc.perform(get("/api/recipes/not-a-uuid"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void createRecipeReturns201WithCreated() throws Exception {
        when(recipeService.createRecipe("Lasagna")).thenReturn(recipe("Lasagna"));

        mockMvc.perform(post("/api/recipes")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\": \"Lasagna\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.name").value("Lasagna"));
    }

    @Test
    void createRecipeReturns400ForBlankName() throws Exception {
        mockMvc.perform(post("/api/recipes")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\": \" \"}"))
                .andExpect(status().isBadRequest());

        verify(recipeService, never()).createRecipe(anyString());
    }

    @Test
    void createRecipeReturns409WhenNameExists() throws Exception {
        when(recipeService.createRecipe("Lasagna"))
                .thenThrow(new ResponseStatusException(HttpStatus.CONFLICT, "Recipe already exists"));

        mockMvc.perform(post("/api/recipes")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\": \"Lasagna\"}"))
                .andExpect(status().isConflict());
    }

    @Test
    void createRecipeReturns409WhenUniqueConstraintFails() throws Exception {
        when(recipeService.createRecipe("Lasagna")).thenThrow(new DataIntegrityViolationException("duplicate key"));

        mockMvc.perform(post("/api/recipes")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\": \"Lasagna\"}"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.detail").value("Resource conflicts with existing data"));
    }

    @Test
    void deleteRecipeReturns204() throws Exception {
        UUID id = UUID.randomUUID();

        mockMvc.perform(delete("/api/recipes/{id}", id))
                .andExpect(status().isNoContent());

        verify(recipeService).deleteRecipe(id);
    }

    private static Recipe recipe(String name) {
        Recipe recipe = new Recipe(name);
        recipe.setId(UUID.randomUUID());
        return recipe;
    }
}
