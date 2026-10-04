package family.cookbook.image;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.HttpStatus;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.multipart.MaxUploadSizeExceededException;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import java.util.Optional;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(ImageController.class)
class ImageControllerTest {

    private static final MockMultipartFile PHOTO =
            new MockMultipartFile("file", "pie.jpg", "image/jpeg", ImageStorageTest.JPEG);

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private ImageStorage storage;

    @Test
    void uploadReturnsTheUrlOfTheStoredPhoto() throws Exception {
        when(storage.store(any(MultipartFile.class))).thenReturn("/api/images/3f9a1c7b-0000-4000-8000-000000000001");

        mockMvc.perform(multipart("/api/images").file(PHOTO))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.url").value("/api/images/3f9a1c7b-0000-4000-8000-000000000001"));
    }

    @Test
    void uploadExplainsWhenTheFileIsNotAPhoto() throws Exception {
        when(storage.store(any(MultipartFile.class))).thenThrow(new ResponseStatusException(HttpStatus.UNSUPPORTED_MEDIA_TYPE,
                "Only JPEG, PNG, WebP and GIF photos can be uploaded"));

        mockMvc.perform(multipart("/api/images").file(PHOTO))
                .andExpect(status().isUnsupportedMediaType())
                .andExpect(jsonPath("$.detail").value("Only JPEG, PNG, WebP and GIF photos can be uploaded"));
    }

    @Test
    void uploadAnswers413WhenTheFileIsTooBig() throws Exception {
        when(storage.store(any(MultipartFile.class))).thenThrow(new MaxUploadSizeExceededException(15 * 1024 * 1024));

        mockMvc.perform(multipart("/api/images").file(PHOTO))
                .andExpect(status().isPayloadTooLarge());
    }

    @Test
    void uploadNeedsAFile() throws Exception {
        mockMvc.perform(multipart("/api/images"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void servesAStoredPhotoWithItsTypeAndLongCaching() throws Exception {
        UUID id = UUID.randomUUID();
        when(storage.find(id)).thenReturn(Optional.of(new Image("image/png", new byte[] {1, 2, 3})));

        mockMvc.perform(get("/api/images/" + id))
                .andExpect(status().isOk())
                .andExpect(content().contentType("image/png"))
                .andExpect(header().string("Cache-Control", "max-age=31536000, public, immutable"))
                .andExpect(content().bytes(new byte[] {1, 2, 3}));
    }

    @Test
    void answers404ForAnUnknownPhoto() throws Exception {
        when(storage.find(any())).thenReturn(Optional.empty());

        mockMvc.perform(get("/api/images/" + UUID.randomUUID()))
                .andExpect(status().isNotFound());
    }

    @Test
    void answers400ForAMalformedId() throws Exception {
        mockMvc.perform(get("/api/images/lecso.jpg"))
                .andExpect(status().isBadRequest());
    }
}
