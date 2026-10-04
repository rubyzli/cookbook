package family.cookbook;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.CacheControl;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.nio.file.Path;

// Serves the files in cookbook.images-dir at /images/..., so a recipe's imageUrl can be "/images/lecso.jpg"
@Configuration
public class ImagesConfig implements WebMvcConfigurer {

    private final Path imagesDir;

    public ImagesConfig(@Value("${cookbook.images-dir}") Path imagesDir) {
        this.imagesDir = imagesDir.toAbsolutePath().normalize();
    }

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        // toUri() gives "file:/.../" with a trailing slash, which Spring needs to treat it as a folder.
        // no-cache: browsers revalidate each time (a cheap 304), so a replaced photo shows up at once
        registry.addResourceHandler("/images/**")
                .addResourceLocations(imagesDir.toUri().toString())
                .setCacheControl(CacheControl.noCache());
    }
}
