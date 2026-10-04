package family.cookbook.importer;

import family.cookbook.importer.dto.ImportedRecipe;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;
import java.util.Optional;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(RecipeImportController.class)
class RecipeImportControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private RecipeImportService importService;

    @Test
    void answersWithADraftInTheVisitorsLanguageContext() throws Exception {
        when(importService.importFrom("https://site.example/r", Optional.of("de"))).thenReturn(new ImportedRecipe(
                "Lecsó", null, 4, null, null, "Kochen.", null, "https://site.example/r", "hu", List.of(), List.of(),
                true, List.of()));

        mockMvc.perform(post("/api/recipes/import")
                        .header("Accept-Language", "de")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"url\": \"https://site.example/r\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Lecsó"))
                .andExpect(jsonPath("$.recipeFound").value(true));
    }

    @Test
    void explainsFailuresWithACode() throws Exception {
        when(importService.importFrom("https://site.example/r", Optional.empty()))
                .thenThrow(new RecipeImportException(HttpStatus.BAD_GATEWAY, "BLOCKED", "The site refused the download (status 403)"));

        mockMvc.perform(post("/api/recipes/import")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"url\": \"https://site.example/r\"}"))
                .andExpect(status().isBadGateway())
                .andExpect(jsonPath("$.code").value("BLOCKED"))
                .andExpect(jsonPath("$.detail").value("The site refused the download (status 403)"));
    }

    @Test
    void needsAUrl() throws Exception {
        mockMvc.perform(post("/api/recipes/import")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"url\": \" \"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors.url").exists());
    }
}
