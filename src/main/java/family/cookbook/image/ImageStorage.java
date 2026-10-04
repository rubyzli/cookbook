package family.cookbook.image;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.SecureRandom;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.HexFormat;

// Saves uploaded photos into the images folder that ImagesConfig serves at /images/...
@Component
public class ImageStorage {

    private static final SecureRandom RANDOM = new SecureRandom();

    private final Path imagesDir;

    public ImageStorage(@Value("${cookbook.images-dir}") Path imagesDir) {
        this.imagesDir = imagesDir.toAbsolutePath().normalize();
    }

    // Returns the URL the photo is served at. The file name is made up here, never taken from the
    // upload, so an upload can't overwrite another photo or land outside the folder.
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
        ImageType type = ImageType.detect(content).orElseThrow(() ->
                new ResponseStatusException(HttpStatus.UNSUPPORTED_MEDIA_TYPE, "Only JPEG, PNG, WebP and GIF photos can be uploaded"));
        try {
            Files.createDirectories(imagesDir);
            Path target = imagesDir.resolve(newName(type));
            Files.write(target, content);
            return "/images/" + target.getFileName();
        } catch (IOException e) {
            throw new UncheckedIOException("Couldn't save the photo", e);
        }
    }

    // e.g. "upload-20261004-3f9a1c7b.jpg": sortable by date, and the random part keeps names unique
    private static String newName(ImageType type) {
        byte[] random = new byte[4];
        RANDOM.nextBytes(random);
        return "upload-" + LocalDate.now().format(DateTimeFormatter.BASIC_ISO_DATE) + "-"
                + HexFormat.of().formatHex(random) + "." + type.extension;
    }
}
