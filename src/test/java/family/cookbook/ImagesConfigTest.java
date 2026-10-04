package family.cookbook;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.forwardedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(FrontendController.class)
@Import(ImagesConfig.class)
class ImagesConfigTest {

    @TempDir
    static Path imagesDir;

    @DynamicPropertySource
    static void imagesDir(DynamicPropertyRegistry registry) {
        registry.add("cookbook.images-dir", imagesDir::toString);
    }

    @Autowired
    private MockMvc mockMvc;

    @Test
    void servesFilesFromTheImagesFolder() throws Exception {
        Files.write(imagesDir.resolve("lecso.jpg"), new byte[] {1, 2, 3});

        mockMvc.perform(get("/images/lecso.jpg"))
                .andExpect(status().isOk())
                .andExpect(content().contentType("image/jpeg"))
                .andExpect(header().string("Cache-Control", "no-cache"))
                .andExpect(content().bytes(new byte[] {1, 2, 3}));
    }

    @Test
    void returns404ForMissingImagesInsteadOfTheApp() throws Exception {
        mockMvc.perform(get("/images/missing.jpg"))
                .andExpect(status().isNotFound())
                .andExpect(forwardedUrl(null));
    }

    @Test
    void doesNotServeFilesOutsideTheFolder() throws Exception {
        Path outside = Files.writeString(imagesDir.getParent().resolve("secret-" + System.nanoTime() + ".txt"), "secret");
        try {
            mockMvc.perform(get("/images/../" + outside.getFileName()))
                    .andExpect(status().isNotFound());
        } finally {
            Files.deleteIfExists(outside);
        }
    }
}
