package family.cookbook.nutrition;

import java.time.Instant;

// estimate is null until one is made. outdated: the ingredients changed after it was made.
// enabled: estimates can be made (an API key is set).
public record NutritionResponse(NutritionEstimate estimate, Instant estimatedAt, boolean outdated, boolean enabled) {
}
