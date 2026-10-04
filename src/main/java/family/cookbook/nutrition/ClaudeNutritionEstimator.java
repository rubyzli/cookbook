package family.cookbook.nutrition;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Map;

// Asks Claude (https://docs.claude.com) to estimate a recipe's nutrition, from the server so the key
// never reaches the browser. A forced tool call makes the answer come back as structured numbers.
@Component
public class ClaudeNutritionEstimator implements NutritionEstimator {

    static final String TOOL = "record_nutrition";

    private static final Map<String, String> LANGUAGE_NAMES = Map.of("hu", "Hungarian", "de", "German", "en", "English");

    private static final String SYSTEM_PROMPT = """
            You estimate the nutrition of home-cooked recipes from their ingredient lists.
            Give the totals for the whole recipe as written: all ingredients together, not per serving.
            Units may be Hungarian or German: dkg = 10 g, ek/EL/evőkanál = tablespoon,
            kk/mk/tk/TL/kiskanál/teáskanál = teaspoon, púpozott = heaped, csapott = level,
            db/Stk. = piece, csomag/zacskó/tasak/Päckchen = a standard retail packet, dl = 100 ml,
            fej = head, gerezd = clove, szál = stalk. Use typical sizes for pieces and packets.
            For a range such as "3–4 ek", use the middle. Ingredients without an amount (e.g. salt,
            pepper, oil for greasing) count only if they would add meaningful calories.
            Use standard food composition values and round to whole numbers.""";

    private final String apiKey;
    private final String model;
    private final RestClient client;

    @Autowired
    public ClaudeNutritionEstimator(@Value("${cookbook.anthropic.api-key:}") String apiKey,
                                    @Value("${cookbook.anthropic.model:claude-sonnet-5-5}") String model) {
        this(apiKey, model, RestClient.builder());
    }

    // Tests pass a builder bound to a mock server
    ClaudeNutritionEstimator(String apiKey, String model, RestClient.Builder builder) {
        this.apiKey = apiKey.strip();
        this.model = model;
        this.client = builder.baseUrl("https://api.anthropic.com").build();
    }

    @Override
    public boolean isEnabled() {
        return !apiKey.isEmpty();
    }

    @Override
    public NutritionEstimate estimate(String recipeName, String language, List<String> ingredientLines) {
        if (!isEnabled()) {
            throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE, "Nutrition estimates are not set up");
        }
        Map<String, Object> body = Map.of(
                "model", model,
                "max_tokens", 1024,
                "system", SYSTEM_PROMPT,
                "tools", List.of(Map.of(
                        "name", TOOL,
                        "description", "Records the estimated nutrition of the whole recipe",
                        "input_schema", Map.of(
                                "type", "object",
                                "properties", Map.of(
                                        "kcal", grams("Energy in kilocalories"),
                                        "protein_g", grams("Protein in grams"),
                                        "carbohydrates_g", grams("Carbohydrates in grams"),
                                        "fat_g", grams("Fat in grams")),
                                "required", List.of("kcal", "protein_g", "carbohydrates_g", "fat_g")))),
                "tool_choice", Map.of("type", "tool", "name", TOOL),
                "messages", List.of(Map.of("role", "user", "content", prompt(recipeName, language, ingredientLines))));
        try {
            ClaudeResponse response = client.post()
                    .uri("/v1/messages")
                    .header("x-api-key", apiKey)
                    .header("anthropic-version", "2023-06-01")
                    .body(body)
                    .retrieve()
                    .onStatus(HttpStatusCode::isError, (request, reply) -> {
                        throw problem(reply.getStatusCode().value());
                    })
                    .body(ClaudeResponse.class);
            return toEstimate(response);
        } catch (RestClientException e) {
            throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, "The nutrition service can't be reached", e);
        }
    }

    static String prompt(String recipeName, String language, List<String> ingredientLines) {
        return "Recipe: " + recipeName + "\n"
                + "Language: " + LANGUAGE_NAMES.getOrDefault(language, language) + "\n"
                + "Ingredients:\n" + String.join("\n", ingredientLines);
    }

    private static Map<String, Object> grams(String description) {
        return Map.of("type", "number", "minimum", 0, "description", description);
    }

    private static NutritionEstimate toEstimate(ClaudeResponse response) {
        Map<String, Object> input = response == null || response.content() == null ? null : response.content().stream()
                .filter(block -> "tool_use".equals(block.type()) && TOOL.equals(block.name()))
                .map(ClaudeResponse.Block::input)
                .findFirst()
                .orElse(null);
        if (input == null) {
            throw unexpected();
        }
        return new NutritionEstimate(number(input, "kcal"), number(input, "protein_g"),
                number(input, "carbohydrates_g"), number(input, "fat_g"));
    }

    private static int number(Map<String, Object> input, String key) {
        if (!(input.get(key) instanceof Number value) || value.doubleValue() < 0) {
            throw unexpected();
        }
        return (int) Math.round(value.doubleValue());
    }

    private static ResponseStatusException unexpected() {
        return new ResponseStatusException(HttpStatus.BAD_GATEWAY, "Unexpected answer from the nutrition service");
    }

    // Turns Claude's error codes into messages that say what to do
    static ResponseStatusException problem(int status) {
        return switch (status) {
            case 401, 403 -> new ResponseStatusException(HttpStatus.BAD_GATEWAY,
                    "The nutrition service rejected the API key");
            case 429, 529 -> new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE,
                    "The nutrition service is busy, try again in a moment");
            default -> new ResponseStatusException(HttpStatus.BAD_GATEWAY,
                    "The nutrition service failed (status " + status + ")");
        };
    }

    record ClaudeResponse(List<Block> content) {
        record Block(String type, String name, Map<String, Object> input) {
        }
    }
}
