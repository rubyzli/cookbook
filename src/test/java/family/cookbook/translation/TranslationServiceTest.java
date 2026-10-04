package family.cookbook.translation;

import family.cookbook.category.Category;
import family.cookbook.category.CategoryRepository;
import family.cookbook.ingredient.Ingredient;
import family.cookbook.ingredient.IngredientRepository;
import family.cookbook.recipe.Recipe;
import family.cookbook.recipe.RecipeIngredient;
import family.cookbook.recipe.RecipeRepository;
import family.cookbook.translation.dto.RecipeTranslationRequest;
import family.cookbook.translation.dto.RecipeTranslationResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class TranslationServiceTest {

    // Prefixes each text with its target language, and records what was asked
    static class FakeTranslator implements MachineTranslator {
        final List<String> calls = new ArrayList<>();

        @Override
        public boolean isEnabled() {
            return true;
        }

        @Override
        public List<String> translate(List<String> texts, String source, String target) {
            calls.add(source + "->" + target + " " + texts);
            return texts.stream().map(t -> t == null || t.isBlank() ? t : "[" + target + "] " + t).toList();
        }
    }

    private final RecipeRepository recipeRepository = mock(RecipeRepository.class);
    private final CategoryRepository categoryRepository = mock(CategoryRepository.class);
    private final IngredientRepository ingredientRepository = mock(IngredientRepository.class);
    private final RecipeTranslationRepository recipeTranslations = mock(RecipeTranslationRepository.class);
    private final CategoryTranslationRepository categoryTranslations = mock(CategoryTranslationRepository.class);
    private final IngredientTranslationRepository ingredientTranslations = mock(IngredientTranslationRepository.class);
    private final FakeTranslator translator = new FakeTranslator();
    private TranslationService service;

    private Recipe recipe;
    private Ingredient potato;
    private Ingredient mehl;
    private Category salad;

    @BeforeEach
    void setUp() {
        service = new TranslationService(recipeRepository, categoryRepository, ingredientRepository,
                recipeTranslations, categoryTranslations, ingredientTranslations, translator);
        potato = withId(new Ingredient("krumpli", "hu"));
        mehl = withId(new Ingredient("Mehl", "de"));
        salad = withId(new Category("Saláta", "hu"));
        recipe = new Recipe("Krumpli saláta");
        recipe.setId(UUID.randomUUID());
        recipe.setInstructions("Főzzük meg.\n\nVágjuk fel.");
        recipe.setNotes("Hidegen jó.");
        recipe.replaceCategories(List.of(salad));
        recipe.replaceIngredients(List.of(
                new RecipeIngredient(recipe, potato, BigDecimal.ONE, "kg", "A salátához", 0),
                new RecipeIngredient(recipe, mehl, null, null, "A salátához", 1),
                new RecipeIngredient(recipe, potato, null, null, null, 2)));
        when(recipeRepository.findWithDetailsById(recipe.getId())).thenReturn(Optional.of(recipe));
        when(recipeTranslations.findById(any())).thenReturn(Optional.empty());
        when(ingredientTranslations.findByIdLanguage(any())).thenReturn(List.of());
        when(categoryTranslations.findByIdLanguage(any())).thenReturn(List.of());
    }

    @Test
    void machineTranslateStoresAMachineTranslationKeepingStepLinesAndGroups() {
        RecipeTranslationResponse response = service.machineTranslate(recipe.getId(), "de").orElseThrow();

        assertThat(response.status()).isEqualTo(TranslationStatus.MACHINE);
        assertThat(response.outdated()).isFalse();
        assertThat(response.name()).isEqualTo("[de] Krumpli saláta");
        assertThat(response.description()).isNull();
        assertThat(response.instructions()).isEqualTo("[de] Főzzük meg.\n\n[de] Vágjuk fel.");
        assertThat(response.notes()).isEqualTo("[de] Hidegen jó.");
        assertThat(response.groups()).isEqualTo(Map.of("A salátához", "[de] A salátához"));
        ArgumentCaptor<RecipeTranslation> saved = ArgumentCaptor.forClass(RecipeTranslation.class);
        verify(recipeTranslations).save(saved.capture());
        assertThat(saved.getValue().getSourceHash()).isEqualTo(SourceText.hash(recipe));
    }

    @Test
    void machineTranslateAlsoTranslatesMissingIngredientAndCategoryNamesFromTheirOwnLanguage() {
        service.machineTranslate(recipe.getId(), "en");

        // Recipe text from Hungarian, then names grouped by the language they were entered in
        assertThat(translator.calls).anyMatch(call -> call.startsWith("hu->en [krumpli]"));
        assertThat(translator.calls).anyMatch(call -> call.startsWith("de->en [Mehl]"));
        assertThat(translator.calls).anyMatch(call -> call.startsWith("hu->en [Saláta]"));
        ArgumentCaptor<IngredientTranslation> names = ArgumentCaptor.forClass(IngredientTranslation.class);
        verify(ingredientTranslations, org.mockito.Mockito.times(2)).save(names.capture());
        assertThat(names.getAllValues()).extracting(IngredientTranslation::getName)
                .containsExactlyInAnyOrder("[en] krumpli", "[en] Mehl");
        assertThat(names.getAllValues()).extracting(IngredientTranslation::getStatus).containsOnly(TranslationStatus.MACHINE);
    }

    @Test
    void machineTranslateSkipsNamesThatAreTranslatedOrAlreadyInThatLanguage() {
        when(ingredientTranslations.findByIdLanguage("de")).thenReturn(
                List.of(new IngredientTranslation(potato.getId(), "de", "Kartoffeln", TranslationStatus.REVIEWED)));

        service.machineTranslate(recipe.getId(), "de");

        // krumpli has a German name already and Mehl is German: no ingredient names to translate
        verify(ingredientTranslations, never()).save(any());
        verify(categoryTranslations).save(any());
    }

    @Test
    void machineTranslateRejectsTheRecipesOwnLanguage() {
        assertThatThrownBy(() -> service.machineTranslate(recipe.getId(), "hu"))
                .isInstanceOfSatisfying(ResponseStatusException.class,
                        e -> assertThat(e.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST));
        assertThat(translator.calls).isEmpty();
    }

    @Test
    void machineTranslateReturnsEmptyForAMissingRecipe() {
        UUID missing = UUID.randomUUID();
        when(recipeRepository.findWithDetailsById(missing)).thenReturn(Optional.empty());

        assertThat(service.machineTranslate(missing, "de")).isEmpty();
    }

    private RecipeTranslation existingTranslation(String language, TranslationStatus status, String sourceHash) {
        RecipeTranslation translation = new RecipeTranslation(recipe.getId(), language);
        translation.update("x", null, null, null, Map.of(), status, sourceHash);
        return translation;
    }

    @Test
    void automaticTranslationCoversTheOtherLanguagesOfANewRecipe() {
        when(recipeTranslations.findByIdOwnerIdOrderByIdLanguage(recipe.getId())).thenReturn(List.of());

        assertThat(service.languagesToTranslateAutomatically(recipe.getId())).containsExactly("de", "en");
    }

    @Test
    void automaticTranslationRedoesOnlyOutdatedMachineTranslations() {
        String current = SourceText.hash(recipe);
        when(recipeTranslations.findByIdOwnerIdOrderByIdLanguage(recipe.getId())).thenReturn(List.of(
                existingTranslation("de", TranslationStatus.MACHINE, "older text"),
                existingTranslation("en", TranslationStatus.MACHINE, current)));

        assertThat(service.languagesToTranslateAutomatically(recipe.getId())).containsExactly("de");
    }

    @Test
    void automaticTranslationNeverReplacesAReviewedTranslation() {
        when(recipeTranslations.findByIdOwnerIdOrderByIdLanguage(recipe.getId())).thenReturn(List.of(
                existingTranslation("de", TranslationStatus.REVIEWED, "older text")));

        assertThat(service.languagesToTranslateAutomatically(recipe.getId())).containsExactly("en");
    }

    @Test
    void automaticTranslationNeedsATranslatorAndAnExistingRecipe() {
        MachineTranslator disabled = mock(MachineTranslator.class);
        TranslationService withoutKey = new TranslationService(recipeRepository, categoryRepository, ingredientRepository,
                recipeTranslations, categoryTranslations, ingredientTranslations, disabled);

        assertThat(withoutKey.languagesToTranslateAutomatically(recipe.getId())).isEmpty();
        assertThat(service.languagesToTranslateAutomatically(UUID.randomUUID())).isEmpty();
    }

    @Test
    void saveTranslationStoresAReviewedTranslationForTheCurrentOriginal() {
        RecipeTranslationRequest request = new RecipeTranslationRequest(" Kartoffelsalat ", "  ", "Kochen.\nSchneiden.",
                null, Map.of("A salátához", " Für den Salat ", "Gone", "x"), null);

        RecipeTranslationResponse response = service.saveTranslation(recipe.getId(), "de", request).orElseThrow();

        assertThat(response.status()).isEqualTo(TranslationStatus.REVIEWED);
        assertThat(response.name()).isEqualTo("Kartoffelsalat");
        assertThat(response.description()).isNull();
        assertThat(response.groups()).isEqualTo(Map.of("A salátához", "Für den Salat"));
        assertThat(response.outdated()).isFalse();
        assertThat(translator.calls).isEmpty();
    }

    @Test
    void translationsBecomeOutdatedWhenTheOriginalChanges() {
        RecipeTranslation stored = new RecipeTranslation(recipe.getId(), "de");
        stored.update("Kartoffelsalat", null, null, null, Map.of(), TranslationStatus.REVIEWED, SourceText.hash(recipe));
        when(recipeTranslations.findByIdOwnerIdOrderByIdLanguage(recipe.getId())).thenReturn(List.of(stored));

        assertThat(service.getTranslations(recipe.getId()).orElseThrow().translations().get(0).outdated()).isFalse();

        recipe.setNotes("Melegen is jó.");

        assertThat(service.getTranslations(recipe.getId()).orElseThrow().translations().get(0).outdated()).isTrue();
        assertThat(service.getTranslations(recipe.getId()).orElseThrow().original().groups()).containsExactly("A salátához");
    }

    @Test
    void setIngredientNameStoresAReviewedName() {
        when(ingredientRepository.findById(potato.getId())).thenReturn(Optional.of(potato));
        when(ingredientTranslations.findById(any())).thenReturn(Optional.empty());

        assertThat(service.setIngredientName(potato.getId(), "de", " Kartoffeln ")).isTrue();

        ArgumentCaptor<IngredientTranslation> saved = ArgumentCaptor.forClass(IngredientTranslation.class);
        verify(ingredientTranslations).save(saved.capture());
        assertThat(saved.getValue().getName()).isEqualTo("Kartoffeln");
        assertThat(saved.getValue().getStatus()).isEqualTo(TranslationStatus.REVIEWED);
    }

    @Test
    void setIngredientNameRejectsTheIngredientsOwnLanguageAndMissingOnes() {
        when(ingredientRepository.findById(potato.getId())).thenReturn(Optional.of(potato));
        when(ingredientRepository.findById(mehl.getId())).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.setIngredientName(potato.getId(), "hu", "krumpli"))
                .isInstanceOf(ResponseStatusException.class);
        assertThat(service.setIngredientName(mehl.getId(), "hu", "liszt")).isFalse();
    }

    @Test
    void translateMissingCategoryNamesCountsWhatItTranslated() {
        Category german = withId(new Category("Kuchen", "de"));
        when(categoryRepository.findAll()).thenReturn(List.of(salad, german));

        assertThat(service.translateMissingCategoryNames("de").translated()).isEqualTo(1);
        assertThat(translator.calls).containsExactly("hu->de [Saláta]");
    }

    private static <T> T withId(T entity) {
        try {
            var field = entity.getClass().getDeclaredField("id");
            field.setAccessible(true);
            field.set(entity, UUID.randomUUID());
            return entity;
        } catch (ReflectiveOperationException e) {
            throw new IllegalStateException(e);
        }
    }
}
