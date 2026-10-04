package family.cookbook.translation;

import family.cookbook.recipe.RecipeSaved;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class AutomaticTranslationTest {

    private final TranslationService translationService = mock(TranslationService.class);
    private final AutomaticTranslation automaticTranslation = new AutomaticTranslation(translationService);
    private final UUID recipeId = UUID.randomUUID();

    @Test
    void translatesIntoEachLanguageThatNeedsIt() {
        when(translationService.languagesToTranslateAutomatically(recipeId)).thenReturn(List.of("de", "en"));

        automaticTranslation.recipeSaved(new RecipeSaved(recipeId));

        verify(translationService).machineTranslate(recipeId, "de");
        verify(translationService).machineTranslate(recipeId, "en");
    }

    @Test
    void aFailedLanguageDoesNotStopTheOthers() {
        when(translationService.languagesToTranslateAutomatically(recipeId)).thenReturn(List.of("de", "en"));
        when(translationService.machineTranslate(recipeId, "de")).thenThrow(new ResponseStatusException(
                HttpStatus.SERVICE_UNAVAILABLE, "This month's machine translation allowance is used up"));
        when(translationService.machineTranslate(recipeId, "en")).thenReturn(Optional.empty());

        automaticTranslation.recipeSaved(new RecipeSaved(recipeId));

        verify(translationService).machineTranslate(recipeId, "en");
    }

    @Test
    void doesNothingWhenNoLanguageNeedsIt() {
        when(translationService.languagesToTranslateAutomatically(recipeId)).thenReturn(List.of());

        automaticTranslation.recipeSaved(new RecipeSaved(recipeId));

        verify(translationService, never()).machineTranslate(recipeId, "de");
        verify(translationService, never()).machineTranslate(recipeId, "en");
    }
}
