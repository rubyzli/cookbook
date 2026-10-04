package family.cookbook.category;

import family.cookbook.category.dto.CategoryUsage;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface CategoryRepository extends JpaRepository<Category, UUID> {

    @Query("""
            select new family.cookbook.category.dto.CategoryUsage(c.id, c.name, c.language,
                (select count(r) from Recipe r join r.categories rc where rc = c))
            from Category c
            """)
    List<CategoryUsage> findAllWithRecipeCount();

    Optional<Category> findByNameIgnoreCase(String name);

    boolean existsByNameIgnoreCase(String name);

    boolean existsByNameIgnoreCaseAndIdNot(String name, UUID id);
}
