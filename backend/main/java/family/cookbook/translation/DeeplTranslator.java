package family.cookbook.translation;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.server.ResponseStatusException;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

// Calls the DeepL API (https://developers.deepl.com) from the server, so the key never reaches the browser.
// Free keys end in ":fx" and use api-free.deepl.com; others use api.deepl.com.
@Component
public class DeeplTranslator implements MachineTranslator {

    // DeepL accepts up to 50 texts per request
    static final int BATCH_SIZE = 50;

    private final String apiKey;
    private final RestClient client;

    @Autowired
    public DeeplTranslator(@Value("${cookbook.deepl.api-key:}") String apiKey) {
        this(apiKey, RestClient.builder());
    }

    // Tests pass a builder bound to a mock server
    DeeplTranslator(String apiKey, RestClient.Builder builder) {
        this.apiKey = apiKey.strip();
        String host = this.apiKey.endsWith(":fx") ? "https://api-free.deepl.com" : "https://api.deepl.com";
        this.client = builder.baseUrl(host).build();
    }

    @Override
    public boolean isEnabled() {
        return !apiKey.isEmpty();
    }

    @Override
    public List<String> translate(List<String> texts, String sourceLanguage, String targetLanguage) {
        if (!isEnabled()) {
            throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE, "Machine translation is not set up");
        }
        List<String> result = new ArrayList<>(texts);
        List<Integer> toSend = new ArrayList<>();
        for (int i = 0; i < texts.size(); i++) {
            if (texts.get(i) != null && !texts.get(i).isBlank()) toSend.add(i);
        }
        for (int start = 0; start < toSend.size(); start += BATCH_SIZE) {
            List<Integer> batch = toSend.subList(start, Math.min(start + BATCH_SIZE, toSend.size()));
            List<String> translated = request(batch.stream().map(texts::get).toList(), sourceLanguage, targetLanguage);
            for (int i = 0; i < batch.size(); i++) {
                result.set(batch.get(i), translated.get(i));
            }
        }
        return result;
    }

    private List<String> request(List<String> texts, String sourceLanguage, String targetLanguage) {
        Map<String, Object> body = Map.of(
                "text", texts,
                "source_lang", sourceLanguage.toUpperCase(),
                "target_lang", deeplTarget(targetLanguage),
                // Recipe texts are short and informal; keep the original sentence and line structure
                "preserve_formatting", true);
        try {
            DeeplResponse response = client.post()
                    .uri("/v2/translate")
                    .header("Authorization", "DeepL-Auth-Key " + apiKey)
                    .body(body)
                    .retrieve()
                    .onStatus(HttpStatusCode::isError, (request, reply) -> {
                        throw problem(reply.getStatusCode().value());
                    })
                    .body(DeeplResponse.class);
            if (response == null || response.translations() == null || response.translations().size() != texts.size()) {
                throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, "Unexpected answer from the translation service");
            }
            return response.translations().stream().map(DeeplResponse.Translation::text).toList();
        } catch (RestClientException e) {
            throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, "The translation service can't be reached", e);
        }
    }

    // DeepL needs a variant for English as a target language
    static String deeplTarget(String language) {
        return "en".equals(language) ? "EN-GB" : language.toUpperCase();
    }

    // Turns DeepL's error codes into messages that say what to do
    static ResponseStatusException problem(int deeplStatus) {
        return switch (deeplStatus) {
            case 403 -> new ResponseStatusException(HttpStatus.BAD_GATEWAY, "The translation service rejected the API key");
            case 456 -> new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE,
                    "This month's machine translation allowance is used up");
            case 429 -> new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE,
                    "The translation service is busy, try again in a moment");
            default -> new ResponseStatusException(HttpStatus.BAD_GATEWAY,
                    "The translation service failed (status " + deeplStatus + ")");
        };
    }

    record DeeplResponse(List<Translation> translations) {
        record Translation(String text) {
        }
    }
}
