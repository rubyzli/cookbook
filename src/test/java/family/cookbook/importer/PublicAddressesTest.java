package family.cookbook.importer;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.net.URI;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class PublicAddressesTest {

    @ParameterizedTest
    @ValueSource(strings = {
            "http://localhost/recipe",
            "http://127.0.0.1/",
            "http://10.0.0.5/",
            "http://172.16.3.4/",
            "http://192.168.1.1/",
            "http://169.254.169.254/latest/meta-data/",
            "http://100.64.0.1/",
            "http://0.0.0.0/",
            "http://[::1]/",
            "http://[fd00::1]/",
            "http://[::ffff:192.168.1.1]/",
            "https://93.184.216.34:5432/",
    })
    void refusesAddressesOnlyTheServerCanReach(String url) {
        assertThatThrownBy(() -> PublicAddresses.requirePublic(URI.create(url)))
                .isInstanceOfSatisfying(RecipeImportException.class,
                        e -> assertThat(e.getCode()).isEqualTo("ADDRESS_NOT_ALLOWED"));
    }

    @Test
    void allowsPublicAddressesOnWebPorts() {
        assertThatCode(() -> PublicAddresses.requirePublic(URI.create("https://93.184.216.34/recipe"))).doesNotThrowAnyException();
        assertThatCode(() -> PublicAddresses.requirePublic(URI.create("http://93.184.216.34:80/"))).doesNotThrowAnyException();
    }

    @ParameterizedTest
    @ValueSource(strings = {"ftp://example.com/x", "file:///etc/passwd", "javascript:alert(1)", "not a url", "https://"})
    void acceptsOnlyHttpWebAddresses(String url) {
        assertThatThrownBy(() -> PublicAddresses.parseWebUrl(url))
                .isInstanceOfSatisfying(RecipeImportException.class, e -> assertThat(e.getCode()).isEqualTo("INVALID_URL"));
    }

    @Test
    void mapsSiteRefusalsToBlocked() {
        assertThat(HttpPageFetcher.failure(403).getCode()).isEqualTo("BLOCKED");
        assertThat(HttpPageFetcher.failure(429).getCode()).isEqualTo("BLOCKED");
        assertThat(HttpPageFetcher.failure(404).getCode()).isEqualTo("PAGE_NOT_FOUND");
        assertThat(HttpPageFetcher.failure(500).getCode()).isEqualTo("SITE_ERROR");
    }
}
