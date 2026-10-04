package family.cookbook.ingredient;

import family.cookbook.ingredient.dto.IngredientListItem;
import family.cookbook.ingredient.dto.IngredientUsage;
import family.cookbook.translation.TranslationLookup;
import family.cookbook.translation.TranslationLookup.TranslatedName;
import family.cookbook.translation.TranslationStatus;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Map;
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

    @Mock
    private TranslationLookup translationLookup;

    @InjectMocks
    private IngredientService ingredientService;

    @Test
    void getAllIngredientsReturnsOriginalNamesSortedWithRecipeCounts() {
        UUID other = UUID.randomUUID();
        UUID main = UUID.randomUUID();
        when(ingredientRepository.findAllWithRecipeCount()).thenReturn(List.of(
                new IngredientUsage(other, "Other", "hu", 0), new IngredientUsage(main, "Flour", "hu", 3)));
        when(translationLookup.allIngredientNames()).thenReturn(Map.of());

        assertThat(ingredientService.getAllIngredients(Optional.empty())).containsExactly(
                new IngredientListItem(main, "Flour", "Flour", "hu", 3, Map.of()),
                new IngredientListItem(other, "Other", "Other", "hu", 0, Map.of()));
    }

    @Test
    void getAllIngredientsShowsTheRequestedTranslationAndSortsByIt() {
        UUID translated = UUID.randomUUID();
        UUID untranslated = UUID.randomUUID();
        Map<String, TranslatedName> names = Map.of("de", new TranslatedName("Mehl", TranslationStatus.MACHINE));
        when(ingredientRepository.findAllWithRecipeCount()).thenReturn(List.of(
                new IngredientUsage(translated, "Flour", "hu", 3), new IngredientUsage(untranslated, "Apfel", "hu", 1)));
        when(translationLookup.allIngredientNames()).thenReturn(Map.of(translated, names));

        assertThat(ingredientService.getAllIngredients(Optional.of("de"))).containsExactly(
                new IngredientListItem(untranslated, "Apfel", "Apfel", "hu", 1, Map.of()),
                new IngredientListItem(translated, "Mehl", "Flour", "hu", 3, names));
    }

    @Test
    void getAllIngredientsKeepsTheOriginalWhenItIsAlreadyInTheRequestedLanguage() {
        UUID id = UUID.randomUUID();
        when(ingredientRepository.findAllWithRecipeCount()).thenReturn(List.of(new IngredientUsage(id, "Mehl", "de", 1)));
        when(translationLookup.allIngredientNames()).thenReturn(
                Map.of(id, Map.of("hu", new TranslatedName("x", TranslationStatus.REVIEWED))));

        assertThat(ingredientService.getAllIngredients(Optional.of("de"))).extracting(IngredientListItem::name).containsExactly("Mehl");
    }

    @Test
    void createIngredientStoresTheLanguageOfTheName() {
        when(ingredientRepository.existsByNameIgnoreCase("Mehl")).thenReturn(false);
        when(ingredientRepository.save(any(Ingredient.class))).thenAnswer(invocation -> invocation.getArgument(0));

        assertThat(ingredientService.createIngredient("Mehl", "de").getLanguage()).isEqualTo("de");
        assertThat(ingredientService.createIngredient("Mehl").getLanguage()).isEqualTo("hu");
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
