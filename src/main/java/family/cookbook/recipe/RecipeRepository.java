package family.cookbook.recipe;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface RecipeRepository extends JpaRepository<Recipe, UUID> {

    // Entity graphs load the associations in the same query instead of one query per recipe

    @EntityGraph(attributePaths = "categories")
    List<Recipe> findByNameContainingIgnoreCaseOrderByNameAsc(String name);

    @EntityGraph(attributePaths = "categories")
    List<Recipe> findDistinctByCategories_IdAndNameContainingIgnoreCaseOrderByNameAsc(UUID categoryId, String name);

    // Fetching categories here too would repeat every ingredient line once per category;
    // they load lazily in a second query instead
    @EntityGraph(attributePaths = {"ingredients", "ingredients.ingredient"})
    Optional<Recipe> findWithDetailsById(UUID id);

    boolean existsByNameIgnoreCase(String name);

    boolean existsByNameIgnoreCaseAndIdNot(String name, UUID id);
}
