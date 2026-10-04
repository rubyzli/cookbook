package family.cookbook.importer;

import family.cookbook.category.Category;
import family.cookbook.category.CategoryRepository;
import family.cookbook.image.ImageStorage;
import family.cookbook.importer.dto.ImportedRecipe;
import family.cookbook.translation.TranslationLookup;
import family.cookbook.translation.TranslationLookup.TranslatedName;
import family.cookbook.translation.TranslationStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.http.HttpStatus;
import tools.jackson.databind.json.JsonMapper;

import java.math.BigDecimal;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.HexFormat;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class RecipeImportServiceTest {

    static final byte[] JPEG = HexFormat.of().parseHex("ffd8ffe000104a46494600");
    static final String PAGE = """
            <html lang="hu"><head><script type="application/ld+json">{
              "@type": "Recipe", "name": "Rakott krumpli", "recipeYield": 6, "prepTime": "PT15M",
              "recipeCategory": "Főétel", "keywords": "Salat, gyors",
              "image": "/img/krumpli.jpg",
              "recipeIngredient": ["1 kg Burgonya", "0 ízlés szerint Só"],
              "recipeInstructions": ["Megfőzzük.", "Rétegezzük."]
            }</script></head></html>
            """;

    // Serves fixed pages and photos by URL, and records nothing else
    static class FakeFetcher implements PageFetcher {
        final Map<String, Download> pages = new java.util.HashMap<>();
        final Map<String, Object> images = new java.util.HashMap<>();

        @Override
        public Download fetchPage(URI uri, String acceptLanguage) {
            Download page = pages.get(uri.toString());
            if (page == null) throw HttpPageFetcher.failure(404);
            return page;
        }

        @Override
        public Download fetchImage(URI uri) {
            Object image = images.get(uri.toString());
            if (image instanceof RecipeImportException e) throw e;
            if (image == null) throw HttpPageFetcher.failure(404);
            return new Download(uri, (byte[]) image, "image/jpeg");
        }
    }

    @TempDir
    Path imagesDir;

    private final FakeFetcher fetcher = new FakeFetcher();
    private final CategoryRepository categories = mock(CategoryRepository.class);
    private final TranslationLookup translations = mock(TranslationLookup.class);
    private RecipeImportService service;
    private Category mainCourse;
    private Category salad;

    @BeforeEach
    void setUp() throws Exception {
        service = new RecipeImportService(fetcher, new ImageStorage(imagesDir), categories, translations,
                JsonMapper.builder().build());
        mainCourse = withId(new Category("Főétel", "hu"));
        salad = withId(new Category("Saláta", "hu"));
        when(categories.findAll()).thenReturn(List.of(mainCourse, salad));
        when(translations.allCategoryNames()).thenReturn(
                Map.of(salad.getId(), Map.of("de", new TranslatedName("Salat", TranslationStatus.MACHINE))));
    }

    private void page(String url, String html) {
        fetcher.pages.put(url, new PageFetcher.Download(URI.create(url), html.getBytes(StandardCharsets.UTF_8),
                "text/html; charset=utf-8"));
    }

    @Test
    void turnsARecipePageIntoAFormDraft() {
        page("https://site.example/rakott", PAGE);
        fetcher.images.put("https://site.example/img/krumpli.jpg", JPEG);

        ImportedRecipe draft = service.importFrom(" https://site.example/rakott ", Optional.of("de"));

        assertThat(draft.recipeFound()).isTrue();
        assertThat(draft.warnings()).isEmpty();
        assertThat(draft.name()).isEqualTo("Rakott krumpli");
        assertThat(draft.servings()).isEqualTo(6);
        assertThat(draft.prepTimeMinutes()).isEqualTo(15);
        assertThat(draft.instructions()).isEqualTo("Megfőzzük.\nRétegezzük.");
        assertThat(draft.language()).isEqualTo("hu");
        assertThat(draft.sourceUrl()).isEqualTo("https://site.example/rakott");
        assertThat(draft.ingredients()).containsExactly(
                new ImportedRecipe.Ingredient(new BigDecimal("1"), "kg", "burgonya"),
                new ImportedRecipe.Ingredient(null, "ízlés szerint", "só"));
        // "Főétel" by name, "Salat" through the German translation of "Saláta"; "gyors" matches nothing
        assertThat(draft.categoryIds()).containsExactly(mainCourse.getId(), salad.getId());
        assertThat(draft.imageUrl()).matches("/images/upload-\\d{8}-[0-9a-f]{8}\\.jpg");
        assertThat(imagesDir.toFile().list()).hasSize(1);
    }

    @Test
    void linksThePhotoWhenItCannotBeDownloaded() {
        page("https://site.example/rakott", PAGE);
        fetcher.images.put("https://site.example/img/krumpli.jpg", HttpPageFetcher.failure(403));

        ImportedRecipe draft = service.importFrom("https://site.example/rakott", Optional.empty());

        assertThat(draft.imageUrl()).isEqualTo("https://site.example/img/krumpli.jpg");
        assertThat(draft.warnings()).containsExactly("PHOTO_NOT_DOWNLOADED");
        assertThat(imagesDir.toFile().list()).isEmpty();
    }

    @Test
    void takesWhatItCanFromAPageWithoutRecipeData() {
        page("https://site.example/plain", """
                <html><head><meta property="og:title" content="Lecsó"><meta property="og:description" content="Nyári."></head></html>
                """);

        ImportedRecipe draft = service.importFrom("https://site.example/plain", Optional.of("de"));

        assertThat(draft.recipeFound()).isFalse();
        assertThat(draft.warnings()).containsExactly("NO_RECIPE_DATA");
        assertThat(draft.name()).isEqualTo("Lecsó");
        assertThat(draft.ingredients()).isEmpty();
        // The page doesn't say, so the visitor's language
        assertThat(draft.language()).isEqualTo("de");
    }

    @Test
    void refusesAPageWithNothingToTake() {
        page("https://site.example/empty", "<html><body>hi</body></html>");

        assertThatThrownBy(() -> service.importFrom("https://site.example/empty", Optional.empty()))
                .isInstanceOfSatisfying(RecipeImportException.class, e -> {
                    assertThat(e.getCode()).isEqualTo("NO_RECIPE_DATA");
                    assertThat(e.getStatusCode()).isEqualTo(HttpStatus.UNPROCESSABLE_CONTENT);
                });
    }

    @Test
    void shortensLongDescriptionsAtAWord() {
        page("https://site.example/long", "<html><head><script type=\"application/ld+json\">{\"@type\": \"Recipe\", "
                + "\"name\": \"X\", \"description\": \"" + "szó ".repeat(100) + "\"}</script></head></html>");

        ImportedRecipe draft = service.importFrom("https://site.example/long", Optional.empty());

        assertThat(draft.description()).hasSizeLessThanOrEqualTo(255).endsWith("szó…");
    }

    @Test
    void readsPagesInOtherEncodings() {
        String html = "<html><head><meta charset=\"iso-8859-2\"><title>Túrós csusza</title></head></html>";
        fetcher.pages.put("https://site.example/old", new PageFetcher.Download(URI.create("https://site.example/old"),
                html.getBytes(java.nio.charset.Charset.forName("ISO-8859-2")), "text/html"));

        assertThat(service.importFrom("https://site.example/old", Optional.empty()).name()).isEqualTo("Túrós csusza");
    }

    private static Category withId(Category category) throws Exception {
        var field = Category.class.getDeclaredField("id");
        field.setAccessible(true);
        field.set(category, UUID.randomUUID());
        return category;
    }
}
