package family.cookbook.ingredient;

import family.cookbook.translation.Languages;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;

import java.util.UUID;

@Entity
public class Ingredient {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;
    @Column(nullable = false)
    private String name;
    // Language the name is written in (see Languages)
    @Column(nullable = false, length = 5)
    private String language;

    protected Ingredient() {
    }

    public Ingredient(String name) {
        this(name, Languages.DEFAULT);
    }

    public Ingredient(String name, String language) {
        this.name = name;
        this.language = language;
    }

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getLanguage() {
        return language;
    }
}
