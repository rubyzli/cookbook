package family.cookbook;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.forwardedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(FrontendController.class)
class FrontendControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @ParameterizedTest
    @ValueSource(strings = {
            "/recipes",
            "/recipes/new",
            "/recipes/3758fcd2-43ff-4564-97ad-029db714069b",
            "/recipes/3758fcd2-43ff-4564-97ad-029db714069b/edit",
            "/apiary",
    })
    void forwardsAppRoutesToIndexHtml(String path) throws Exception {
        mockMvc.perform(get(path))
                .andExpect(status().isOk())
                .andExpect(forwardedUrl("/index.html"));
    }

    @ParameterizedTest
    @ValueSource(strings = {"/api", "/api/unknown", "/api/recipes/x/unknown"})
    void leavesUnknownApiPathsAsNotFound(String path) throws Exception {
        mockMvc.perform(get(path))
                .andExpect(status().isNotFound())
                .andExpect(forwardedUrl(null));
    }

    // Whether these exist depends on whether the frontend has been built into static/,
    // so only check that they're not turned into the app page
    @ParameterizedTest
    @ValueSource(strings = {"/assets/index-abc123.js", "/assets", "/favicon.svg", "/images/lecso.jpg", "/images"})
    void leavesFilesAlone(String path) throws Exception {
        mockMvc.perform(get(path))
                .andExpect(forwardedUrl(null));
    }
}
