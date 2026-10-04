package family.cookbook.translation;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;

import java.io.Serializable;
import java.util.Objects;
import java.util.UUID;

// Primary key of a translation: what is translated (recipe, ingredient or category id) and into which language
@Embeddable
public class TranslationKey implements Serializable {

    @Column(name = "owner_id")
    private UUID ownerId;
    @Column(name = "language", length = 5)
    private String language;

    protected TranslationKey() {
    }

    public TranslationKey(UUID ownerId, String language) {
        this.ownerId = ownerId;
        this.language = language;
    }

    public UUID getOwnerId() {
        return ownerId;
    }

    public String getLanguage() {
        return language;
    }

    @Override
    public boolean equals(Object other) {
        return other instanceof TranslationKey key && ownerId.equals(key.ownerId) && language.equals(key.language);
    }

    @Override
    public int hashCode() {
        return Objects.hash(ownerId, language);
    }
}
