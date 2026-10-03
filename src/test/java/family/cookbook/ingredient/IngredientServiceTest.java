package family.cookbook.ingredient;

import family.cookbook.ingredient.dto.IngredientListItem;
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
    void getAllIngredientsReturnsListWithRecipeCounts() {
        List<IngredientListItem> ingredients = List.of(
                new IngredientListItem(UUID.randomUUID(), "Flour", 3),
                new IngredientListItem(UUID.randomUUID(), "Other", 0));
        when(ingredientRepository.findAllWithRecipeCount()).thenReturn(ingredients);

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

    @Test
    void renameIngredientSavesNewName() {
        UUID id = UUID.randomUUID();
        Ingredient ingredient = new Ingredient("Flour");
        when(ingredientRepository.findById(id)).thenReturn(Optional.of(ingredient));
        when(ingredientRepository.existsByNameIgnoreCaseAndIdNot("Renamed", id)).thenReturn(false);
        when(ingredientRepository.save(ingredient)).thenReturn(ingredient);

        assertThat(ingredientService.renameIngredient(id, "Renamed")).hasValueSatisfying(
                renamed -> assertThat(renamed.getName()).isEqualTo("Renamed"));
    }

    @Test
    void renameIngredientReturnsEmptyWhenMissing() {
        UUID id = UUID.randomUUID();
        when(ingredientRepository.findById(id)).thenReturn(Optional.empty());

        assertThat(ingredientService.renameIngredient(id, "Renamed")).isEmpty();
        verify(ingredientRepository, never()).save(any());
    }

    @Test
    void renameIngredientRejectsNameUsedByAnotherWithConflict() {
        UUID id = UUID.randomUUID();
        Ingredient ingredient = new Ingredient("Flour");
        when(ingredientRepository.findById(id)).thenReturn(Optional.of(ingredient));
        when(ingredientRepository.existsByNameIgnoreCaseAndIdNot("Taken", id)).thenReturn(true);

        assertThatThrownBy(() -> ingredientService.renameIngredient(id, "Taken"))
                .isInstanceOfSatisfying(ResponseStatusException.class,
                        ex -> assertThat(ex.getStatusCode()).isEqualTo(HttpStatus.CONFLICT));
        assertThat(ingredient.getName()).isEqualTo("Flour");
        verify(ingredientRepository, never()).save(any());
    }

    @Test
    void deleteIngredientRejectsIngredientInUseWithConflict() {
        UUID id = UUID.randomUUID();
        when(ingredientRepository.countRecipesUsing(id)).thenReturn(2L);

        assertThatThrownBy(() -> ingredientService.deleteIngredient(id))
                .isInstanceOfSatisfying(ResponseStatusException.class, ex -> {
                    assertThat(ex.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
                    assertThat(ex.getReason()).isEqualTo("Ingredient is used by 2 recipes");
                });
        verify(ingredientRepository, never()).deleteById(any());
    }

    @Test
    void deleteIngredientSaysRecipeForOne() {
        UUID id = UUID.randomUUID();
        when(ingredientRepository.countRecipesUsing(id)).thenReturn(1L);

        assertThatThrownBy(() -> ingredientService.deleteIngredient(id))
                .hasMessageContaining("Ingredient is used by 1 recipe\"");
    }
}
