package family.cookbook.translation.dto;

import java.util.List;

// machineTranslation: whether an API key is configured, so the "translate automatically" buttons work
public record TranslationSettings(boolean machineTranslation, List<String> languages) {
}
