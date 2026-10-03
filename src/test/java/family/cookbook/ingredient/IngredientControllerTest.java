package family.cookbook.ingredient;

import family.cookbook.ingredient.dto.IngredientListItem;
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
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(IngredientController.class)
class IngredientControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private IngredientService ingredientService;

    @Test
    void getAllIngredientsReturnsListWithRecipeCounts() throws Exception {
        UUID id = UUID.randomUUID();
        when(ingredientService.getAllIngredients()).thenReturn(List.of(new IngredientListItem(id, "Flour", 3)));

        mockMvc.perform(get("/api/ingredients"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].id").value(id.toString()))
                .andExpect(jsonPath("$[0].name").value("Flour"))
                .andExpect(jsonPath("$[0].recipeCount").value(3));
    }

    @Test
    void getIngredientByIdReturnsMatch() throws Exception {
        Ingredient ingredient = ingredient("Flour");
        when(ingredientService.getIngredientById(ingredient.getId())).thenReturn(Optional.of(ingredient));

        mockMvc.perform(get("/api/ingredients/{id}", ingredient.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(ingredient.getId().toString()))
                .andExpect(jsonPath("$.name").value("Flour"));
    }

    @Test
    void getIngredientByIdReturns404WhenMissing() throws Exception {
        when(ingredientService.getIngredientById(any())).thenReturn(Optional.empty());

        mockMvc.perform(get("/api/ingredients/{id}", UUID.randomUUID()))
                .andExpect(status().isNotFound());
    }

    @Test
    void getIngredientByIdReturns400ForMalformedId() throws Exception {
        mockMvc.perform(get("/api/ingredients/not-a-uuid"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void createIngredientReturns201WithCreated() throws Exception {
        when(ingredientService.createIngredient("Flour")).thenReturn(ingredient("Flour"));

        mockMvc.perform(post("/api/ingredients")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\": \"Flour\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.name").value("Flour"));
    }

    @Test
    void createIngredientReturns400ForBlankName() throws Exception {
        mockMvc.perform(post("/api/ingredients")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\": \" \"}"))
                .andExpect(status().isBadRequest());

        verify(ingredientService, never()).createIngredient(anyString());
    }

    @Test
    void createIngredientReturns409WhenNameExists() throws Exception {
        when(ingredientService.createIngredient("Flour"))
                .thenThrow(new ResponseStatusException(HttpStatus.CONFLICT, "Ingredient already exists"));

        mockMvc.perform(post("/api/ingredients")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\": \"Flour\"}"))
                .andExpect(status().isConflict());
    }

    @Test
    void createIngredientReturns409WhenUniqueConstraintFails() throws Exception {
        when(ingredientService.createIngredient("Flour")).thenThrow(new DataIntegrityViolationException("duplicate key"));

        mockMvc.perform(post("/api/ingredients")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\": \"Flour\"}"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.detail").value("Resource conflicts with existing data"));
    }

    @Test
    void deleteIngredientReturns204() throws Exception {
        UUID id = UUID.randomUUID();

        mockMvc.perform(delete("/api/ingredients/{id}", id))
                .andExpect(status().isNoContent());

        verify(ingredientService).deleteIngredient(id);
    }

    private static Ingredient ingredient(String name) {
        Ingredient ingredient = new Ingredient(name);
        ingredient.setId(UUID.randomUUID());
        return ingredient;
    }

    @Test
    void renameIngredientReturnsRenamed() throws Exception {
        Ingredient renamed = ingredient("Renamed");
        when(ingredientService.renameIngredient(renamed.getId(), "Renamed")).thenReturn(Optional.of(renamed));

        mockMvc.perform(put("/api/ingredients/{id}", renamed.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\": \"Renamed\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Renamed"));
    }

    @Test
    void renameIngredientReturns404WhenMissing() throws Exception {
        when(ingredientService.renameIngredient(any(), anyString())).thenReturn(Optional.empty());

        mockMvc.perform(put("/api/ingredients/{id}", UUID.randomUUID())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\": \"Renamed\"}"))
                .andExpect(status().isNotFound());
    }

    @Test
    void renameIngredientReturns400ForBlankName() throws Exception {
        mockMvc.perform(put("/api/ingredients/{id}", UUID.randomUUID())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\": \"\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors.name").exists());

        verify(ingredientService, never()).renameIngredient(any(), anyString());
    }

    @Test
    void renameIngredientReturns409WhenNameTaken() throws Exception {
        when(ingredientService.renameIngredient(any(), anyString()))
                .thenThrow(new ResponseStatusException(HttpStatus.CONFLICT, "Ingredient already exists"));

        mockMvc.perform(put("/api/ingredients/{id}", UUID.randomUUID())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\": \"Taken\"}"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.detail").value("Ingredient already exists"));
    }

    @Test
    void deleteIngredientReturns409WithReasonWhenInUse() throws Exception {
        UUID id = UUID.randomUUID();
        doThrow(new ResponseStatusException(HttpStatus.CONFLICT, "Ingredient is used by 2 recipes"))
                .when(ingredientService).deleteIngredient(id);

        mockMvc.perform(delete("/api/ingredients/{id}", id))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.detail").value("Ingredient is used by 2 recipes"));
    }
}
