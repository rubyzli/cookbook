package family.cookbook.nutrition;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface RecipeNutritionRepository extends JpaRepository<RecipeNutrition, UUID> {
}
