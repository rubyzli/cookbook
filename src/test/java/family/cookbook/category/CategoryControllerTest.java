package family.cookbook.category;

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

@WebMvcTest(CategoryController.class)
class CategoryControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private CategoryService categoryService;

    @Test
    void getAllCategoriesReturnsList() throws Exception {
        when(categoryService.getAllCategories()).thenReturn(List.of(category("Desserts")));

        mockMvc.perform(get("/api/categories"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].name").value("Desserts"));
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
        when(categoryService.createCategory("Desserts")).thenReturn(category("Desserts"));

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

        verify(categoryService, never()).createCategory(anyString());
    }

    @Test
    void createCategoryReturns409WhenNameExists() throws Exception {
        when(categoryService.createCategory("Desserts"))
                .thenThrow(new ResponseStatusException(HttpStatus.CONFLICT, "Category already exists"));

        mockMvc.perform(post("/api/categories")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\": \"Desserts\"}"))
                .andExpect(status().isConflict());
    }

    @Test
    void createCategoryReturns409WhenUniqueConstraintFails() throws Exception {
        when(categoryService.createCategory("Desserts")).thenThrow(new DataIntegrityViolationException("duplicate key"));

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
}
