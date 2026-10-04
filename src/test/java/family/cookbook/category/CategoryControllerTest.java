package family.cookbook.category;

import family.cookbook.category.dto.CategoryListItem;
import family.cookbook.translation.TranslationLookup.TranslatedName;
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

import java.util.List;
import java.util.Map;
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
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(CategoryController.class)
class CategoryControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private CategoryService categoryService;

    @Test
    void getAllCategoriesReturnsListWithRecipeCounts() throws Exception {
        UUID id = UUID.randomUUID();
        when(categoryService.getAllCategories(Optional.empty())).thenReturn(List.of(new CategoryListItem(id, "Desserts", "Desserts", "hu", 3,
                Map.of("de", new TranslatedName("Nachspeisen", TranslationStatus.MACHINE)))));

        mockMvc.perform(get("/api/categories"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].id").value(id.toString()))
                .andExpect(jsonPath("$[0].name").value("Desserts"))
                .andExpect(jsonPath("$[0].recipeCount").value(3))
                .andExpect(jsonPath("$[0].originalLanguage").value("hu"))
                .andExpect(jsonPath("$[0].translations.de.name").value("Nachspeisen"))
                .andExpect(jsonPath("$[0].translations.de.status").value("MACHINE"));
    }

    @Test
    void getAllCategoriesPassesTheLanguageFromAcceptLanguage() throws Exception {
        UUID id = UUID.randomUUID();
        when(categoryService.getAllCategories(Optional.of("de")))
                .thenReturn(List.of(new CategoryListItem(id, "Nachspeisen", "Desserts", "hu", 3, Map.of())));

        mockMvc.perform(get("/api/categories").header("Accept-Language", "fr-FR, de-AT;q=0.9, en;q=0.5"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].name").value("Nachspeisen"));
    }

    @Test
    void createCategoryPassesTheLanguage() throws Exception {
        when(categoryService.createCategory("Nachspeisen", "de")).thenReturn(category("Nachspeisen"));

        mockMvc.perform(post("/api/categories")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\": \"Nachspeisen\", \"language\": \"de\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.name").value("Nachspeisen"));
    }

    @Test
    void createCategoryReturns400ForUnsupportedLanguage() throws Exception {
        mockMvc.perform(post("/api/categories")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\": \"Farine\", \"language\": \"fr\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors.language").exists());
    }

    @Test
    void getCategoryByIdReturnsMatch() throws Exception {
        Category category = category("Desserts");
        when(categoryService.getCategoryById(category.getId())).thenReturn(Optional.of(category));

        mockMvc.perform(get("/api/categories/{id}", category.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(category.getId().toString()))
                .andExpect(jsonPath("$.name").value("Desserts"));
    }

    @Test
    void getCategoryByIdReturns404WhenMissing() throws Exception {
        when(categoryService.getCategoryById(any())).thenReturn(Optional.empty());

        mockMvc.perform(get("/api/categories/{id}", UUID.randomUUID()))
                .andExpect(status().isNotFound());
    }

    @Test
    void getCategoryByIdReturns400ForMalformedId() throws Exception {
        mockMvc.perform(get("/api/categories/not-a-uuid"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void createCategoryReturns201WithCreated() throws Exception {
        when(categoryService.createCategory("Desserts", null)).thenReturn(category("Desserts"));

        mockMvc.perform(post("/api/categories")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\": \"Desserts\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.name").value("Desserts"));
    }

    @Test
    void createCategoryReturns400ForBlankName() throws Exception {
        mockMvc.perform(post("/api/categories")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\": \" \"}"))
                .andExpect(status().isBadRequest());

        verify(categoryService, never()).createCategory(anyString(), any());
    }

    @Test
    void createCategoryReturns409WhenNameExists() throws Exception {
        when(categoryService.createCategory("Desserts", null))
                .thenThrow(new ResponseStatusException(HttpStatus.CONFLICT, "Category already exists"));

        mockMvc.perform(post("/api/categories")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\": \"Desserts\"}"))
                .andExpect(status().isConflict());
    }

    @Test
    void createCategoryReturns409WhenUniqueConstraintFails() throws Exception {
        when(categoryService.createCategory("Desserts", null)).thenThrow(new DataIntegrityViolationException("duplicate key"));

        mockMvc.perform(post("/api/categories")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\": \"Desserts\"}"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.detail").value("Resource conflicts with existing data"));
    }

    @Test
    void deleteCategoryReturns204() throws Exception {
        UUID id = UUID.randomUUID();

        mockMvc.perform(delete("/api/categories/{id}", id))
                .andExpect(status().isNoContent());

        verify(categoryService).deleteCategory(id);
    }

    private static Category category(String name) {
        Category category = new Category(name);
        category.setId(UUID.randomUUID());
        return category;
    }

    @Test
    void renameCategoryReturnsRenamed() throws Exception {
        Category renamed = category("Renamed");
        when(categoryService.renameCategory(renamed.getId(), "Renamed")).thenReturn(Optional.of(renamed));

        mockMvc.perform(put("/api/categories/{id}", renamed.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\": \"Renamed\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Renamed"));
    }

    @Test
    void renameCategoryReturns404WhenMissing() throws Exception {
        when(categoryService.renameCategory(any(), anyString())).thenReturn(Optional.empty());

        mockMvc.perform(put("/api/categories/{id}", UUID.randomUUID())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\": \"Renamed\"}"))
                .andExpect(status().isNotFound());
    }

    @Test
    void renameCategoryReturns400ForBlankName() throws Exception {
        mockMvc.perform(put("/api/categories/{id}", UUID.randomUUID())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\": \"\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors.name").exists());

        verify(categoryService, never()).renameCategory(any(), anyString());
    }

    @Test
    void renameCategoryReturns409WhenNameTaken() throws Exception {
        when(categoryService.renameCategory(any(), anyString()))
                .thenThrow(new ResponseStatusException(HttpStatus.CONFLICT, "Category already exists"));

        mockMvc.perform(put("/api/categories/{id}", UUID.randomUUID())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\": \"Taken\"}"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.detail").value("Category already exists"));
    }
}
