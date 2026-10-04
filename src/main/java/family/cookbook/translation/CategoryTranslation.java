package family.cookbook.translation;

import jakarta.persistence.AttributeOverride;
import jakarta.persistence.Column;
import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;

import java.util.UUID;

// The name of a category in another language
@Entity
public class CategoryTranslation {

    @EmbeddedId
    @AttributeOverride(name = "ownerId", column = @Column(name = "category_id"))
    private TranslationKey id;
    @Column(nullable = false)
    private String name;
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private TranslationStatus status;

    protected CategoryTranslation() {
    }

    public CategoryTranslation(UUID categoryId, String language, String name, TranslationStatus status) {
        this.id = new TranslationKey(categoryId, language);
        this.name = name;
        this.status = status;
    }

    public void update(String name, TranslationStatus status) {
        this.name = name;
        this.status = status;
    }

    public UUID getCategoryId() {
        return id.getOwnerId();
    }

    public String getLanguage() {
        return id.getLanguage();
    }

    public String getName() {
        return name;
    }

    public TranslationStatus getStatus() {
        return status;
    }
}
