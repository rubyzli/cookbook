package family.cookbook.translation;

import jakarta.persistence.AttributeOverride;
import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.MapKeyColumn;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@Entity
public class RecipeTranslation {

    @EmbeddedId
    @AttributeOverride(name = "ownerId", column = @Column(name = "recipe_id"))
    private TranslationKey id;
    @Column(nullable = false)
    private String name;
    private String description;
    @Column(columnDefinition = "text")
    private String instructions;
    @Column(columnDefinition = "text")
    private String notes;
    // Original ingredient group heading -> translated heading
    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "recipe_translation_group", joinColumns = {
            @JoinColumn(name = "recipe_id", referencedColumnName = "recipe_id"),
            @JoinColumn(name = "language", referencedColumnName = "language")})
    @MapKeyColumn(name = "original", length = 100)
    @Column(name = "translated", length = 100, nullable = false)
    private Map<String, String> groups = new HashMap<>();
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private TranslationStatus status;
    @Column(nullable = false, length = 64)
    private String sourceHash;
    @Column(nullable = false)
    private Instant updatedAt;

    protected RecipeTranslation() {
    }

    public RecipeTranslation(UUID recipeId, String language) {
        this.id = new TranslationKey(recipeId, language);
    }

    @PrePersist
    @PreUpdate
    void touch() {
        updatedAt = Instant.now();
    }

    // Replaces the translated text, and records which version of the original it belongs to
    public void update(String name, String description, String instructions, String notes,
                       Map<String, String> groups, TranslationStatus status, String sourceHash) {
        this.name = name;
        this.description = description;
        this.instructions = instructions;
        this.notes = notes;
        this.groups.clear();
        this.groups.putAll(groups);
        this.status = status;
        this.sourceHash = sourceHash;
    }

    public UUID getRecipeId() {
        return id.getOwnerId();
    }

    public String getLanguage() {
        return id.getLanguage();
    }

    public String getName() {
        return name;
    }

    public String getDescription() {
        return description;
    }

    public String getInstructions() {
        return instructions;
    }

    public String getNotes() {
        return notes;
    }

    public Map<String, String> getGroups() {
        return groups;
    }

    public TranslationStatus getStatus() {
        return status;
    }

    public String getSourceHash() {
        return sourceHash;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }
}
