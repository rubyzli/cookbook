package family.cookbook.recipe;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RecipeServiceTest {

    @Mock
    private RecipeRepository recipeRepository;

    @InjectMocks
    private RecipeService recipeService;

    @Test
    void getAllRecipesReturnsEverythingFromRepository() {
        List<Recipe> recipes = List.of(new Recipe("Lasagna"), new Recipe("Other"));
        when(recipeRepository.findAll()).thenReturn(recipes);

        assertThat(recipeService.getAllRecipes()).isEqualTo(recipes);
    }

    @Test
    void createRecipeSavesNewNameAndReturnsSaved() {
        when(recipeRepository.existsByNameIgnoreCase("Lasagna")).thenReturn(false);
        when(recipeRepository.save(any(Recipe.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Recipe created = recipeService.createRecipe("Lasagna");

        assertThat(created.getName()).isEqualTo("Lasagna");
        verify(recipeRepository).save(created);
    }

    @Test
    void createRecipeRejectsDuplicateNameWithConflict() {
        when(recipeRepository.existsByNameIgnoreCase("Lasagna")).thenReturn(true);

        assertThatThrownBy(() -> recipeService.createRecipe("Lasagna"))
                .isInstanceOfSatisfying(ResponseStatusException.class,
                        ex -> assertThat(ex.getStatusCode()).isEqualTo(HttpStatus.CONFLICT));
        verify(recipeRepository, never()).save(any());
    }

    @Test
    void getRecipeByIdReturnsMatchFromRepository() {
        UUID id = UUID.randomUUID();
        Recipe recipe = new Recipe("Lasagna");
        when(recipeRepository.findById(id)).thenReturn(Optional.of(recipe));

        assertThat(recipeService.getRecipeById(id)).contains(recipe);
    }

    @Test
    void getRecipeByIdReturnsEmptyWhenMissing() {
        UUID id = UUID.randomUUID();
        when(recipeRepository.findById(id)).thenReturn(Optional.empty());

        assertThat(recipeService.getRecipeById(id)).isEmpty();
    }

    @Test
    void deleteRecipeDeletesById() {
        UUID id = UUID.randomUUID();

        recipeService.deleteRecipe(id);

        verify(recipeRepository).deleteById(id);
    }
}
