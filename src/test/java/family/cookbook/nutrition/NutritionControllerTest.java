package family.cookbook.nutrition;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.HttpStatus;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.server.ResponseStatusException;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(NutritionController.class)
class NutritionControllerTest {

    private static final UUID ID = UUID.fromString("3f9a1c7b-0000-4000-8000-000000000001");

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private NutritionService nutritionService;

    @Test
    void returnsTheEstimate() throws Exception {
        when(nutritionService.getNutrition(ID)).thenReturn(Optional.of(new NutritionResponse(
                new NutritionEstimate(1200, 40, 150, 45), Instant.parse("2026-10-04T10:00:00Z"), true, true)));

        mockMvc.perform(get("/api/recipes/{id}/nutrition", ID))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.estimate.kcal").value(1200))
                .andExpect(jsonPath("$.estimate.proteinGrams").value(40))
                .andExpect(jsonPath("$.estimate.carbsGrams").value(150))
                .andExpect(jsonPath("$.estimate.fatGrams").value(45))
                .andExpect(jsonPath("$.outdated").value(true))
                .andExpect(jsonPath("$.enabled").value(true));
    }

    @Test
    void unknownRecipeIs404() throws Exception {
        when(nutritionService.getNutrition(ID)).thenReturn(Optional.empty());

        mockMvc.perform(get("/api/recipes/{id}/nutrition", ID)).andExpect(status().isNotFound());
    }

    @Test
    void estimatingWithoutAKeyIs503() throws Exception {
        when(nutritionService.estimate(ID)).thenThrow(
                new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE, "Nutrition estimates are not set up"));

        mockMvc.perform(post("/api/recipes/{id}/nutrition", ID)).andExpect(status().isServiceUnavailable());
    }
}
