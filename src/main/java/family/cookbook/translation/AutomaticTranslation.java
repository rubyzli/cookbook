package family.cookbook.translation;

import family.cookbook.recipe.RecipeSaved;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionalEventListener;

// Machine-translates a recipe into the other languages after it's saved, so visitors in another
// language don't have to wait for someone to press "Translate automatically". Runs in the
// background after the save is committed: saving stays fast, and a DeepL failure never undoes it.
@Component
public class AutomaticTranslation {

    private static final Logger log = LoggerFactory.getLogger(AutomaticTranslation.class);

    private final TranslationService translationService;

    public AutomaticTranslation(TranslationService translationService) {
        this.translationService = translationService;
    }

    @Async
    @TransactionalEventListener
    public void recipeSaved(RecipeSaved event) {
        for (String language : translationService.languagesToTranslateAutomatically(event.recipeId())) {
            try {
                translationService.machineTranslate(event.recipeId(), language);
            } catch (RuntimeException e) {
                // The button on the recipe page can still be used later
                log.warn("Couldn't translate recipe {} into {} automatically: {}", event.recipeId(), language,
                        e.getMessage());
            }
        }
    }
}
