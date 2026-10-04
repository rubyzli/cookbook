package family.cookbook.translation;

import java.util.List;

// Translates plain texts between two of the supported languages. The DeepL implementation is used
// when an API key is configured; tests use a fake.
public interface MachineTranslator {

    boolean isEnabled();

    // One translation per text, in the same order. Blank texts come back unchanged without a request.
    List<String> translate(List<String> texts, String sourceLanguage, String targetLanguage);
}
