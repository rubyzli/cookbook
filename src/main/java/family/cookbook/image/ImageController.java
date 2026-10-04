package family.cookbook.image;

import org.springframework.http.CacheControl;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import java.time.Duration;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/images")
public class ImageController {

    private final ImageStorage storage;

    public ImageController(ImageStorage storage) {
        this.storage = storage;
    }

    // multipart/form-data with the photo in a "file" field; answers {"url": "/api/images/..."}, which
    // is what goes into a recipe's imageUrl
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public Map<String, String> upload(@RequestParam("file") MultipartFile file) {
        return Map.of("url", storage.store(file));
    }

    // A stored photo never changes (a replaced one gets a new id), so browsers may keep it for good
    @GetMapping("/{id}")
    public ResponseEntity<byte[]> get(@PathVariable UUID id) {
        Image image = storage.find(id).orElseThrow(() ->
                new ResponseStatusException(HttpStatus.NOT_FOUND, "Photo not found"));
        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(image.getContentType()))
                .cacheControl(CacheControl.maxAge(Duration.ofDays(365)).cachePublic().immutable())
                .body(image.getData());
    }
}
