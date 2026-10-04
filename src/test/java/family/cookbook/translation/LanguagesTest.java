package family.cookbook.translation;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.springframework.web.server.ResponseStatusException;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class LanguagesTest {

    @ParameterizedTest
    @CsvSource(delimiter = '|', value = {
            "de                               | de",
            "de-AT,de;q=0.9,en;q=0.8          | de",
            "fr-FR, hu;q=0.7, en;q=0.5        | hu",
            "EN-us                            | en",
    })
    void fromHeaderPicksTheFirstSupportedLanguage(String header, String expected) {
        assertThat(Languages.fromHeader(header)).contains(expected);
    }

    @Test
    void fromHeaderIsEmptyWithoutASupportedLanguage() {
        assertThat(Languages.fromHeader(null)).isEmpty();
        assertThat(Languages.fromHeader("")).isEmpty();
        assertThat(Languages.fromHeader("fr-FR,es")).isEmpty();
        assertThat(Languages.fromHeader("not a header;;;")).isEqualTo(Optional.empty());
    }

    @Test
    void requireNormalizesAndRejectsUnsupported() {
        assertThat(Languages.require("DE")).isEqualTo("de");
        assertThatThrownBy(() -> Languages.require("fr")).isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("Unsupported language: fr");
    }
}
