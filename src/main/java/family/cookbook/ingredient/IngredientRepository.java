package family.cookbook.ingredient;

import family.cookbook.ingredient.dto.IngredientListItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.UUID;

public interface IngredientRepository extends JpaRepository<Ingredient, UUID> {

    @Query("""
            select new family.cookbook.ingredient.dto.IngredientListItem(i.id, i.name,
                (select count(distinct ri.recipe.id) from RecipeIngredient ri where ri.ingredient = i))
            from Ingredient i
            order by lower(i.name)
            """)
    List<IngredientListItem> findAllWithRecipeCount();

    boolean existsByNameIgnoreCase(String name);

    boolean existsByNameIgnoreCaseAndIdNot(String name, UUID id);

    @Query("select count(distinct ri.recipe.id) from RecipeIngredient ri where ri.ingredient.id = :id")
    long countRecipesUsing(UUID id);
}
