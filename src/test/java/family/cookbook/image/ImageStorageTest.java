package family.cookbook.image;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.springframework.http.HttpStatus;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.web.server.ResponseStatusException;

import java.util.ArrayList;
import java.util.HexFormat;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class ImageStorageTest {

    static final byte[] JPEG = HexFormat.of().parseHex("ffd8ffe000104a46494600");

    // A repository that hands out ids like the database would and remembers what was saved
    static ImageRepository repository(List<Image> saved) {
        ImageRepository images = mock(ImageRepository.class);
        when(images.save(any(Image.class))).thenAnswer(call -> {
            Image image = call.getArgument(0);
            image.setId(UUID.randomUUID());
            saved.add(image);
            return image;
        });
        return images;
    }

    private final List<Image> saved = new ArrayList<>();
    private final ImageStorage storage = new ImageStorage(repository(saved));

    private static MockMultipartFile file(String name, byte[] content) {
        return new MockMultipartFile("file", name, "application/octet-stream", content);
    }

    @Test
    void storesThePhotoAndReturnsItsUrl() {
        String url = storage.store(file("pie.jpg", JPEG));

        assertThat(saved).hasSize(1);
        Image image = saved.get(0);
        assertThat(url).isEqualTo("/api/images/" + image.getId());
        assertThat(image.getData()).isEqualTo(JPEG);
        assertThat(image.getContentType()).isEqualTo("image/jpeg");
    }

    @ParameterizedTest
    @CsvSource({
            "89504e470d0a1a0a0000000d, image/png",
            "474946383961010001000000, image/gif",
            "524946462400000057454250, image/webp",
    })
    void takesTheTypeFromTheContentNotTheName(String hex, String contentType) {
        storage.store(file("photo.jpg", HexFormat.of().parseHex(hex)));

        assertThat(saved.get(0).getContentType()).isEqualTo(contentType);
    }

    @Test
    void refusesFilesThatAreNotPhotos() {
        assertThatThrownBy(() -> storage.store(file("recipe.jpg", "<html>not a photo</html>".getBytes())))
                .isInstanceOfSatisfying(ResponseStatusException.class,
                        e -> assertThat(e.getStatusCode()).isEqualTo(HttpStatus.UNSUPPORTED_MEDIA_TYPE));
        assertThat(saved).isEmpty();
    }

    @Test
    void refusesEmptyFiles() {
        assertThatThrownBy(() -> storage.store(file("empty.jpg", new byte[0])))
                .isInstanceOfSatisfying(ResponseStatusException.class,
                        e -> assertThat(e.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST));
        assertThat(saved).isEmpty();
    }
}
