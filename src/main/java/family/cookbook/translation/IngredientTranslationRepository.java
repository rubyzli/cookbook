package family.cookbook.translation;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface IngredientTranslationRepository extends JpaRepository<IngredientTranslation, TranslationKey> {

    List<IngredientTranslation> findByIdLanguage(String language);

    List<IngredientTranslation> findByIdOwnerId(UUID ingredientId);
}
