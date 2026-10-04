package family.cookbook.image;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.HttpStatus;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.multipart.MaxUploadSizeExceededException;
import org.springframework.web.server.ResponseStatusException;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
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
        when(storage.store(any())).thenReturn("/images/upload-20261004-3f9a1c7b.jpg");

        mockMvc.perform(multipart("/api/images").file(PHOTO))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.url").value("/images/upload-20261004-3f9a1c7b.jpg"));
    }

    @Test
    void uploadExplainsWhenTheFileIsNotAPhoto() throws Exception {
        when(storage.store(any())).thenThrow(new ResponseStatusException(HttpStatus.UNSUPPORTED_MEDIA_TYPE,
                "Only JPEG, PNG, WebP and GIF photos can be uploaded"));

        mockMvc.perform(multipart("/api/images").file(PHOTO))
                .andExpect(status().isUnsupportedMediaType())
                .andExpect(jsonPath("$.detail").value("Only JPEG, PNG, WebP and GIF photos can be uploaded"));
    }

    @Test
    void uploadAnswers413WhenTheFileIsTooBig() throws Exception {
        when(storage.store(any())).thenThrow(new MaxUploadSizeExceededException(15 * 1024 * 1024));

        mockMvc.perform(multipart("/api/images").file(PHOTO))
                .andExpect(status().isPayloadTooLarge());
    }

    @Test
    void uploadNeedsAFile() throws Exception {
        mockMvc.perform(multipart("/api/images"))
                .andExpect(status().isBadRequest());
    }
}
