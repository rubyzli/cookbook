package family.cookbook.image;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.Optional;
import java.util.UUID;

// Saves uploaded photos into the database; ImageController serves them at /api/images/{id}
@Component
public class ImageStorage {

    public static final String URL_PREFIX = "/api/images/";

    private final ImageRepository images;

    public ImageStorage(ImageRepository images) {
        this.images = images;
    }

    // Returns the URL the photo is served at
    public String store(MultipartFile file) {
        if (file.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "The file is empty");
        }
        try {
            return store(file.getBytes());
        } catch (IOException e) {
            throw new UncheckedIOException("Couldn't read the upload", e);
        }
    }

    // The same for a photo already in memory, e.g. downloaded while importing a recipe
    public String store(byte[] content) {
        if (content.length == 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "The file is empty");
        }
        // The type is taken from the content, never from the upload, so a stored photo is always
        // served as what it really is
        ImageType type = ImageType.detect(content).orElseThrow(() ->
                new ResponseStatusException(HttpStatus.UNSUPPORTED_MEDIA_TYPE, "Only JPEG, PNG, WebP and GIF photos can be uploaded"));
        return URL_PREFIX + images.save(new Image(type.contentType, content)).getId();
    }

    public Optional<Image> find(UUID id) {
        return images.findById(id);
    }
}
