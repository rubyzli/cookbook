package family.cookbook.nutrition;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.header;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.jsonPath;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withRawStatus;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

class ClaudeNutritionEstimatorTest {

    private static final List<String> LINES = List.of("A tésztához:", "25 dkg liszt", "1 db tojás");

    private final RestClient.Builder builder = RestClient.builder();
    private final MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
    private final ClaudeNutritionEstimator estimator = new ClaudeNutritionEstimator("secret", "some-model", builder);

    private static String toolAnswer(String input) {
        return """
                {"id": "msg_1", "type": "message", "role": "assistant", "stop_reason": "tool_use",
                 "content": [{"type": "tool_use", "id": "toolu_1", "name": "record_nutrition", "input": %s}]}
                """.formatted(input);
    }

    @Test
    void asksForAForcedToolCallAndReadsTheNumbers() {
        server.expect(requestTo("https://api.anthropic.com/v1/messages"))
                .andExpect(method(HttpMethod.POST))
                .andExpect(header("x-api-key", "secret"))
                .andExpect(header("anthropic-version", "2023-06-01"))
                .andExpect(jsonPath("$.model").value("some-model"))
                .andExpect(jsonPath("$.tool_choice.name").value("record_nutrition"))
                .andExpect(jsonPath("$.messages[0].content").value(
                        "Recipe: Pogácsa\nLanguage: Hungarian\nIngredients:\nA tésztához:\n25 dkg liszt\n1 db tojás"))
                .andRespond(withSuccess(
                        toolAnswer("{\"kcal\": 1010.4, \"protein_g\": 32.6, \"carbohydrates_g\": 190, \"fat_g\": 9}"),
                        MediaType.APPLICATION_JSON));

        NutritionEstimate estimate = estimator.estimate("Pogácsa", "hu", LINES);

        assertThat(estimate).isEqualTo(new NutritionEstimate(1010, 33, 190, 9));
        server.verify();
    }

    @Test
    void rejectsAnAnswerWithoutTheNumbers() {
        server.expect(requestTo("https://api.anthropic.com/v1/messages"))
                .andRespond(withSuccess(toolAnswer("{\"kcal\": 1000}"), MediaType.APPLICATION_JSON));

        assertThatThrownBy(() -> estimator.estimate("Pogácsa", "hu", LINES))
                .isInstanceOfSatisfying(ResponseStatusException.class,
                        e -> assertThat(e.getStatusCode()).isEqualTo(HttpStatus.BAD_GATEWAY));
    }

    @Test
    void explainsARejectedKey() {
        server.expect(requestTo("https://api.anthropic.com/v1/messages")).andRespond(withRawStatus(401));

        assertThatThrownBy(() -> estimator.estimate("Pogácsa", "hu", LINES))
                .hasMessageContaining("rejected the API key");
    }

    @Test
    void reportsBusyAsUnavailable() {
        server.expect(requestTo("https://api.anthropic.com/v1/messages")).andRespond(withRawStatus(529));

        assertThatThrownBy(() -> estimator.estimate("Pogácsa", "hu", LINES))
                .isInstanceOfSatisfying(ResponseStatusException.class,
                        e -> assertThat(e.getStatusCode()).isEqualTo(HttpStatus.SERVICE_UNAVAILABLE));
    }

    @Test
    void isDisabledWithoutAKey() {
        ClaudeNutritionEstimator withoutKey = new ClaudeNutritionEstimator(" ", "some-model", RestClient.builder());

        assertThat(withoutKey.isEnabled()).isFalse();
        assertThatThrownBy(() -> withoutKey.estimate("Pogácsa", "hu", LINES))
                .isInstanceOfSatisfying(ResponseStatusException.class,
                        e -> assertThat(e.getStatusCode()).isEqualTo(HttpStatus.SERVICE_UNAVAILABLE));
    }
}
