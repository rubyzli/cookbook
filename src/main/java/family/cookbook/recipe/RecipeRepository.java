package family.cookbook.recipe;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface RecipeRepository extends JpaRepository<Recipe, UUID> {

    List<Recipe> findByNameContainingIgnoreCase(String name);

    boolean existsByNameIgnoreCase(String name);
}
