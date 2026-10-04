package family.cookbook.translation;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;
import org.springframework.web.server.ResponseStatusException;

import java.util.Arrays;
import java.util.List;
import java.util.stream.IntStream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.header;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.jsonPath;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withRawStatus;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

class DeeplTranslatorTest {

    private final RestClient.Builder builder = RestClient.builder();
    private final MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();

    private static String translations(String... texts) {
        return "{\"translations\": [" + String.join(",", Arrays.stream(texts)
                .map(text -> "{\"detected_source_language\": \"HU\", \"text\": \"" + text + "\"}").toList()) + "]}";
    }

    @Test
    void sendsNonBlankTextsToTheFreeApiAndKeepsTheOrder() {
        DeeplTranslator translator = new DeeplTranslator("secret:fx", builder);
        server.expect(requestTo("https://api-free.deepl.com/v2/translate"))
                .andExpect(method(HttpMethod.POST))
                .andExpect(header("Authorization", "DeepL-Auth-Key secret:fx"))
                .andExpect(jsonPath("$.text.length()").value(2))
                .andExpect(jsonPath("$.text[0]").value("krumpli"))
                .andExpect(jsonPath("$.text[1]").value("hagyma"))
                .andExpect(jsonPath("$.source_lang").value("HU"))
                .andExpect(jsonPath("$.target_lang").value("DE"))
                .andRespond(withSuccess(translations("Kartoffeln", "Zwiebel"), MediaType.APPLICATION_JSON));

        List<String> result = translator.translate(Arrays.asList("krumpli", null, " ", "hagyma"), "hu", "de");

        assertThat(result).containsExactly("Kartoffeln", null, " ", "Zwiebel");
        server.verify();
    }

    @Test
    void usesTheProApiForOtherKeysAndBritishEnglishAsTarget() {
        DeeplTranslator translator = new DeeplTranslator("pro-key", builder);
        server.expect(requestTo("https://api.deepl.com/v2/translate"))
                .andExpect(jsonPath("$.target_lang").value("EN-GB"))
                .andRespond(withSuccess(translations("potato"), MediaType.APPLICATION_JSON));

        assertThat(translator.translate(List.of("krumpli"), "hu", "en")).containsExactly("potato");
        server.verify();
    }

    @Test
    void splitsLongListsIntoBatchesOfFifty() {
        DeeplTranslator translator = new DeeplTranslator("secret:fx", builder);
        List<String> texts = IntStream.range(0, 51).mapToObj(i -> "t" + i).toList();
        server.expect(jsonPath("$.text.length()").value(50))
                .andRespond(withSuccess(translations(texts.subList(0, 50).toArray(String[]::new)), MediaType.APPLICATION_JSON));
        server.expect(jsonPath("$.text.length()").value(1))
                .andRespond(withSuccess(translations("t50"), MediaType.APPLICATION_JSON));

        assertThat(translator.translate(texts, "hu", "de")).isEqualTo(texts);
        server.verify();
    }

    @Test
    void makesNoRequestWhenThereIsNothingToTranslate() {
        DeeplTranslator translator = new DeeplTranslator("secret:fx", builder);

        assertThat(translator.translate(Arrays.asList("", null), "hu", "de")).containsExactly("", null);
        server.verify();
    }

    @Test
    void explainsAUsedUpAllowance() {
        DeeplTranslator translator = new DeeplTranslator("secret:fx", builder);
        server.expect(requestTo("https://api-free.deepl.com/v2/translate")).andRespond(withRawStatus(456));

        assertThatThrownBy(() -> translator.translate(List.of("krumpli"), "hu", "de"))
                .isInstanceOfSatisfying(ResponseStatusException.class, e -> {
                    assertThat(e.getStatusCode()).isEqualTo(HttpStatus.SERVICE_UNAVAILABLE);
                    assertThat(e.getReason()).isEqualTo("This month's machine translation allowance is used up");
                });
    }

    @Test
    void explainsARejectedKey() {
        DeeplTranslator translator = new DeeplTranslator("wrong:fx", builder);
        server.expect(requestTo("https://api-free.deepl.com/v2/translate")).andRespond(withStatus(HttpStatus.FORBIDDEN));

        assertThatThrownBy(() -> translator.translate(List.of("krumpli"), "hu", "de"))
                .isInstanceOfSatisfying(ResponseStatusException.class, e -> {
                    assertThat(e.getStatusCode()).isEqualTo(HttpStatus.BAD_GATEWAY);
                    assertThat(e.getReason()).isEqualTo("The translation service rejected the API key");
                });
    }

    @Test
    void isDisabledWithoutAKey() {
        DeeplTranslator translator = new DeeplTranslator("  ", builder);

        assertThat(translator.isEnabled()).isFalse();
        assertThatThrownBy(() -> translator.translate(List.of("krumpli"), "hu", "de"))
                .isInstanceOfSatisfying(ResponseStatusException.class,
                        e -> assertThat(e.getStatusCode()).isEqualTo(HttpStatus.SERVICE_UNAVAILABLE));
    }
}
