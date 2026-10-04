package family.cookbook.translation;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface CategoryTranslationRepository extends JpaRepository<CategoryTranslation, TranslationKey> {

    List<CategoryTranslation> findByIdLanguage(String language);

    List<CategoryTranslation> findByIdOwnerId(UUID categoryId);
}
