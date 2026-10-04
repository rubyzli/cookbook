package family.cookbook.translation;

import family.cookbook.translation.dto.MachineFillResult;
import family.cookbook.translation.dto.RecipeTranslationRequest;
import family.cookbook.translation.dto.RecipeTranslationResponse;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.server.ResponseStatusException;

import java.time.Instant;
import java.util.Map;
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

@WebMvcTest(TranslationController.class)
class TranslationControllerTest {

    private static final UUID ID = UUID.randomUUID();

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private TranslationService translationService;

    private static RecipeTranslationResponse response(TranslationStatus status) {
        return new RecipeTranslationResponse("de", status, false, "Kartoffelsalat", null, "Kochen.", null,
                Map.of("A salátához", "Für den Salat"), Instant.parse("2026-10-04T12:00:00Z"));
    }

    @Test
    void settingsSayWhetherMachineTranslationIsAvailable() throws Exception {
        when(translationService.machineTranslationEnabled()).thenReturn(true);

        mockMvc.perform(get("/api/translations/settings"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.machineTranslation").value(true))
                .andExpect(jsonPath("$.languages[0]").value("de"))
                .andExpect(jsonPath("$.languages.length()").value(3));
    }

    @Test
    void machineTranslateReturnsTheTranslation() throws Exception {
        when(translationService.machineTranslate(ID, "de")).thenReturn(Optional.of(response(TranslationStatus.MACHINE)));

        mockMvc.perform(post("/api/recipes/{id}/translations/DE/machine", ID))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("MACHINE"))
                .andExpect(jsonPath("$.groups['A salátához']").value("Für den Salat"));
    }

    @Test
    void machineTranslateReturns404ForAMissingRecipeAnd400ForAnUnsupportedLanguage() throws Exception {
        when(translationService.machineTranslate(ID, "de")).thenReturn(Optional.empty());

        mockMvc.perform(post("/api/recipes/{id}/translations/de/machine", ID)).andExpect(status().isNotFound());
        mockMvc.perform(post("/api/recipes/{id}/translations/fr/machine", ID))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detail").value("Unsupported language: fr"));
    }

    @Test
    void machineTranslateShowsWhyItIsUnavailable() throws Exception {
        when(translationService.machineTranslate(ID, "de")).thenThrow(
                new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE, "This month's machine translation allowance is used up"));

        mockMvc.perform(post("/api/recipes/{id}/translations/de/machine", ID))
                .andExpect(status().isServiceUnavailable())
                .andExpect(jsonPath("$.detail").value("This month's machine translation allowance is used up"));
    }

    @Test
    void saveTranslationValidatesAndSaves() throws Exception {
        when(translationService.saveTranslation(eq(ID), eq("de"), any())).thenReturn(Optional.of(response(TranslationStatus.REVIEWED)));

        mockMvc.perform(put("/api/recipes/{id}/translations/de", ID)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\": \"Kartoffelsalat\", \"groups\": {\"A salátához\": \"Für den Salat\"}}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("REVIEWED"));
        verify(translationService).saveTranslation(ID, "de", new RecipeTranslationRequest("Kartoffelsalat", null, null,
                null, Map.of("A salátához", "Für den Salat"), TranslationStatus.REVIEWED));

        mockMvc.perform(put("/api/recipes/{id}/translations/de", ID)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\": \" \"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors.name").exists());
    }

    @Test
    void deleteTranslationReturns204() throws Exception {
        mockMvc.perform(delete("/api/recipes/{id}/translations/de", ID)).andExpect(status().isNoContent());

        verify(translationService).deleteTranslation(ID, "de");
    }

    @Test
    void nameTranslationEndpoints() throws Exception {
        when(translationService.setIngredientName(ID, "de", "Kartoffeln")).thenReturn(true);
        when(translationService.setCategoryName(ID, "de", "Salat")).thenReturn(false);
        when(translationService.translateMissingIngredientNames("en")).thenReturn(new MachineFillResult(4));

        mockMvc.perform(put("/api/ingredients/{id}/translations/de", ID)
                        .contentType(MediaType.APPLICATION_JSON).content("{\"name\": \"Kartoffeln\"}"))
                .andExpect(status().isNoContent());
        mockMvc.perform(put("/api/categories/{id}/translations/de", ID)
                        .contentType(MediaType.APPLICATION_JSON).content("{\"name\": \"Salat\"}"))
                .andExpect(status().isNotFound());
        mockMvc.perform(post("/api/ingredients/translations/en/machine"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.translated").value(4));
        mockMvc.perform(delete("/api/categories/{id}/translations/hu", ID)).andExpect(status().isNoContent());
        verify(translationService).deleteCategoryName(ID, "hu");
        verify(translationService, never()).setIngredientName(any(), eq("fr"), any());
    }
}
