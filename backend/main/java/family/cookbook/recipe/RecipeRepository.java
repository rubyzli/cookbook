package family.cookbook.recipe;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface RecipeRepository extends JpaRepository<Recipe, UUID> {

    // Entity graphs load the associations in the same query instead of one query per recipe

    // Matches the original name or the name translated into `language`; sorting happens after
    // localizing, by the name that is shown
    @EntityGraph(attributePaths = "categories")
    @Query("""
            select distinct r from Recipe r
            left join RecipeTranslation t on t.id.ownerId = r.id and t.id.language = :language
            where lower(r.name) like lower(concat('%', :search, '%'))
               or lower(t.name) like lower(concat('%', :search, '%'))
            """)
    List<Recipe> search(String search, String language);

    @EntityGraph(attributePaths = "categories")
    @Query("""
            select distinct r from Recipe r
            join r.categories c
            left join RecipeTranslation t on t.id.ownerId = r.id and t.id.language = :language
            where c.id = :categoryId
              and (lower(r.name) like lower(concat('%', :search, '%'))
                   or lower(t.name) like lower(concat('%', :search, '%')))
            """)
    List<Recipe> searchInCategory(String search, String language, UUID categoryId);

    // Fetching categories here too would repeat every ingredient line once per category;
    // they load lazily in a second query instead
    @EntityGraph(attributePaths = {"ingredients", "ingredients.ingredient"})
    Optional<Recipe> findWithDetailsById(UUID id);

    boolean existsByNameIgnoreCase(String name);

    boolean existsByNameIgnoreCaseAndIdNot(String name, UUID id);
}
