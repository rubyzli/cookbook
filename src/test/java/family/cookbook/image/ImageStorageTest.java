package family.cookbook.image;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.springframework.http.HttpStatus;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.web.server.ResponseStatusException;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HexFormat;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ImageStorageTest {

    static final byte[] JPEG = HexFormat.of().parseHex("ffd8ffe000104a46494600");

    @TempDir
    Path imagesDir;

    private static MockMultipartFile file(String name, byte[] content) {
        return new MockMultipartFile("file", name, "application/octet-stream", content);
    }

    @Test
    void storesTheFileUnderAGeneratedNameAndReturnsItsUrl() throws IOException {
        ImageStorage storage = new ImageStorage(imagesDir);

        String url = storage.store(file("../../etc/passwd.jpg", JPEG));

        assertThat(url).matches("/images/upload-\\d{8}-[0-9a-f]{8}\\.jpg");
        Path saved = imagesDir.resolve(url.substring("/images/".length()));
        assertThat(Files.readAllBytes(saved)).isEqualTo(JPEG);
        assertThat(saved.getParent()).isEqualTo(imagesDir.toAbsolutePath().normalize());
    }

    @ParameterizedTest
    @CsvSource({
            "89504e470d0a1a0a0000000d, png",
            "474946383961010001000000, gif",
            "524946462400000057454250, webp",
    })
    void picksTheExtensionFromTheContentNotTheName(String hex, String extension) {
        ImageStorage storage = new ImageStorage(imagesDir);

        assertThat(storage.store(file("photo.jpg", HexFormat.of().parseHex(hex)))).endsWith("." + extension);
    }

    @Test
    void refusesFilesThatAreNotPhotos() {
        ImageStorage storage = new ImageStorage(imagesDir);

        assertThatThrownBy(() -> storage.store(file("recipe.jpg", "<html>not a photo</html>".getBytes())))
                .isInstanceOfSatisfying(ResponseStatusException.class,
                        e -> assertThat(e.getStatusCode()).isEqualTo(HttpStatus.UNSUPPORTED_MEDIA_TYPE));
        assertThat(imagesDir.toFile().list()).isEmpty();
    }

    @Test
    void refusesEmptyFiles() {
        ImageStorage storage = new ImageStorage(imagesDir);

        assertThatThrownBy(() -> storage.store(file("empty.jpg", new byte[0])))
                .isInstanceOfSatisfying(ResponseStatusException.class,
                        e -> assertThat(e.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST));
    }

    @Test
    void createsTheFolderAndNeverReusesANameWithinADay() {
        ImageStorage storage = new ImageStorage(imagesDir.resolve("not-there-yet"));

        String first = storage.store(file("a.jpg", JPEG));
        String second = storage.store(file("a.jpg", JPEG));

        assertThat(first).isNotEqualTo(second);
        assertThat(imagesDir.resolve("not-there-yet").toFile().list()).hasSize(2);
    }
}
