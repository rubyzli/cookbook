package family.cookbook.nutrition;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/api/recipes/{id}/nutrition")
public class NutritionController {

    private final NutritionService nutritionService;

    public NutritionController(NutritionService nutritionService) {
        this.nutritionService = nutritionService;
    }

    @GetMapping
    public ResponseEntity<NutritionResponse> getNutrition(@PathVariable UUID id) {
        return ResponseEntity.of(nutritionService.getNutrition(id));
    }

    // Estimates again now, e.g. for recipes saved before estimates were set up
    @PostMapping
    public ResponseEntity<NutritionResponse> estimate(@PathVariable UUID id) {
        return ResponseEntity.of(nutritionService.estimate(id));
    }
}
