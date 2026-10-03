package family.cookbook.ingredient;

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
class IngredientServiceTest {

    @Mock
    private IngredientRepository ingredientRepository;

    @InjectMocks
    private IngredientService ingredientService;

    @Test
    void getAllIngredientsReturnsEverythingFromRepository() {
        List<Ingredient> ingredients = List.of(new Ingredient("Flour"), new Ingredient("Other"));
        when(ingredientRepository.findAll()).thenReturn(ingredients);

        assertThat(ingredientService.getAllIngredients()).isEqualTo(ingredients);
    }

    @Test
    void createIngredientSavesNewNameAndReturnsSaved() {
        when(ingredientRepository.existsByNameIgnoreCase("Flour")).thenReturn(false);
        when(ingredientRepository.save(any(Ingredient.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Ingredient created = ingredientService.createIngredient("Flour");

        assertThat(created.getName()).isEqualTo("Flour");
        verify(ingredientRepository).save(created);
    }

    @Test
    void createIngredientRejectsDuplicateNameWithConflict() {
        when(ingredientRepository.existsByNameIgnoreCase("Flour")).thenReturn(true);

        assertThatThrownBy(() -> ingredientService.createIngredient("Flour"))
                .isInstanceOfSatisfying(ResponseStatusException.class,
                        ex -> assertThat(ex.getStatusCode()).isEqualTo(HttpStatus.CONFLICT));
        verify(ingredientRepository, never()).save(any());
    }

    @Test
    void getIngredientByIdReturnsMatchFromRepository() {
        UUID id = UUID.randomUUID();
        Ingredient ingredient = new Ingredient("Flour");
        when(ingredientRepository.findById(id)).thenReturn(Optional.of(ingredient));

        assertThat(ingredientService.getIngredientById(id)).contains(ingredient);
    }

    @Test
    void getIngredientByIdReturnsEmptyWhenMissing() {
        UUID id = UUID.randomUUID();
        when(ingredientRepository.findById(id)).thenReturn(Optional.empty());

        assertThat(ingredientService.getIngredientById(id)).isEmpty();
    }

    @Test
    void deleteIngredientDeletesById() {
        UUID id = UUID.randomUUID();

        ingredientService.deleteIngredient(id);

        verify(ingredientRepository).deleteById(id);
    }
}
