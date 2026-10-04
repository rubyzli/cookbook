package family.cookbook.translation;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.List;
import java.util.UUID;

public interface RecipeTranslationRepository extends JpaRepository<RecipeTranslation, TranslationKey> {

    List<RecipeTranslation> findByIdLanguageAndIdOwnerIdIn(String language, Collection<UUID> recipeIds);

    List<RecipeTranslation> findByIdOwnerIdOrderByIdLanguage(UUID recipeId);
}
