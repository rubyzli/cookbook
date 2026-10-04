package family.cookbook.importer;

import family.cookbook.importer.RecipePageParser.ParsedRecipe;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.json.JsonMapper;

import static org.assertj.core.api.Assertions.assertThat;

class RecipePageParserTest {

    private final ObjectMapper json = JsonMapper.builder().build();

    private ParsedRecipe parse(String html) {
        return RecipePageParser.parse(html, "https://example.com/recept/rakott-krumpli", json);
    }

    // Shaped like nosalty.hu: steps as plain texts, image as a list of ImageObjects
    @Test
    void readsJsonLdWithTextStepsAndImageObjects() {
        ParsedRecipe recipe = parse("""
                <html lang="hu"><head>
                <script type="application/ld+json">{
                  "@context": "https://schema.org", "@type": "Recipe", "name": "Rakott krumpli", "inLanguage": "hu",
                  "description": "Klasszikus.", "recipeYield": 6, "prepTime": "PT15M", "cookTime": "PT1H30M",
                  "recipeCategory": "Főétel", "keywords": "Burgonya, Tojás",
                  "image": [{"@type": "ImageObject", "url": "https://img.example.com/krumpli.jpeg?w=1200"}],
                  "recipeIngredient": ["1 kg Burgonya", " 6 db Főtt tojás "],
                  "recipeInstructions": ["Héjában főzzük a krumplit.", "Olajat öntünk a tepsibe."]
                }</script></head><body></body></html>
                """);

        assertThat(recipe.recipeData()).isTrue();
        assertThat(recipe.name()).isEqualTo("Rakott krumpli");
        assertThat(recipe.description()).isEqualTo("Klasszikus.");
        assertThat(recipe.servings()).isEqualTo(6);
        assertThat(recipe.prepTimeMinutes()).isEqualTo(15);
        assertThat(recipe.cookTimeMinutes()).isEqualTo(90);
        assertThat(recipe.ingredientLines()).containsExactly("1 kg Burgonya", "6 db Főtt tojás");
        assertThat(recipe.steps()).containsExactly("Héjában főzzük a krumplit.", "Olajat öntünk a tepsibe.");
        assertThat(recipe.imageUrls()).containsExactly("https://img.example.com/krumpli.jpeg?w=1200");
        assertThat(recipe.categoryHints()).containsExactly("Főétel", "Burgonya", "Tojás");
        assertThat(recipe.language()).isEqualTo("hu");
    }

    // Shaped like mindmegette.hu: HowToSteps with entities, site name in the title, no inLanguage
    @Test
    void readsHowToStepsDropsTheSiteNameAndUsesThePageLanguage() {
        ParsedRecipe recipe = parse("""
                <html lang="hu-HU"><head><meta property="og:site_name" content="Mindmegette">
                <script type="application/ld+json">[{"@type": "WebPage"}, {
                  "@type": "Recipe", "name": "Rakott krumpli | Mindmegette.hu", "recipeYield": "4 adag",
                  "image": "https://cdn.example.com/k.jpg",
                  "recipeInstructions": [
                    {"@type": "HowToStep", "name": "1. lépés", "text": "A krumplit megfőzzük.&nbsp;"},
                    {"@type": "HowToStep", "name": "2. lépés", "text": "<p>Rétegezzük.</p>"}
                  ]
                }]</script></head></html>
                """);

        assertThat(recipe.name()).isEqualTo("Rakott krumpli");
        assertThat(recipe.servings()).isEqualTo(4);
        assertThat(recipe.steps()).containsExactly("A krumplit megfőzzük.", "Rétegezzük.");
        assertThat(recipe.language()).isEqualTo("hu-HU");
    }

    @Test
    void findsTheRecipeInAGraphAndKeepsSectionHeadings() {
        ParsedRecipe recipe = parse("""
                <script type="application/ld+json">{ this is broken }</script>
                <script type="application/ld+json">{"@graph": [{"@type": "Organization"}, {
                  "@type": ["Recipe", "NewsArticle"], "name": "Linzer",
                  "recipeInstructions": [
                    {"@type": "HowToSection", "name": "A tésztához", "itemListElement": [
                      {"@type": "HowToStep", "text": "Összegyúrjuk."}]},
                    {"@type": "HowToSection", "name": "A töltelékhez:", "itemListElement": [
                      {"@type": "HowToStep", "text": "Megkenjük."}]}
                  ]}]}</script>
                """);

        assertThat(recipe.name()).isEqualTo("Linzer");
        assertThat(recipe.steps()).containsExactly("A tésztához:", "Összegyúrjuk.", "A töltelékhez:", "Megkenjük.");
    }

    @Test
    void readsMicrodata() {
        ParsedRecipe recipe = parse("""
                <div itemscope itemtype="https://schema.org/Recipe">
                  <h1 itemprop="name">Pancakes</h1>
                  <img itemprop="image" src="/img/pancakes.jpg">
                  <meta itemprop="prepTime" content="PT10M">
                  <span itemprop="recipeYield">Serves 4</span>
                  <ul><li itemprop="recipeIngredient">200 g flour</li><li itemprop="recipeIngredient">2 eggs</li></ul>
                  <ol itemprop="recipeInstructions"><li>Mix.</li><li>Fry.</li></ol>
                </div>
                """);

        assertThat(recipe.recipeData()).isTrue();
        assertThat(recipe.name()).isEqualTo("Pancakes");
        assertThat(recipe.imageUrls()).containsExactly("https://example.com/img/pancakes.jpg");
        assertThat(recipe.prepTimeMinutes()).isEqualTo(10);
        assertThat(recipe.servings()).isEqualTo(4);
        assertThat(recipe.ingredientLines()).containsExactly("200 g flour", "2 eggs");
        assertThat(recipe.steps()).containsExactly("Mix.", "Fry.");
    }

    @Test
    void fallsBackToThePagesOwnTitleDescriptionAndPhoto() {
        ParsedRecipe recipe = parse("""
                <html lang="hu"><head><title>ignored</title>
                <meta property="og:title" content="Rakott krumpli – Street Kitchen">
                <meta property="og:site_name" content="Street Kitchen">
                <meta property="og:description" content="Brutál jó.">
                <meta property="og:image" content="https://cdn.example.com/rk.webp">
                </head></html>
                """);

        assertThat(recipe.recipeData()).isFalse();
        assertThat(recipe.name()).isEqualTo("Rakott krumpli");
        assertThat(recipe.description()).isEqualTo("Brutál jó.");
        assertThat(recipe.imageUrls()).containsExactly("https://cdn.example.com/rk.webp");
        assertThat(recipe.ingredientLines()).isEmpty();
    }

    @Test
    void readsDurationsInSeveralShapes() {
        assertThat(RecipePageParser.minutes("PT1H30M")).isEqualTo(90);
        assertThat(RecipePageParser.minutes("pt45m")).isEqualTo(45);
        assertThat(RecipePageParser.minutes("45 perc")).isEqualTo(45);
        assertThat(RecipePageParser.minutes("PT0M")).isNull();
        assertThat(RecipePageParser.minutes(null)).isNull();
    }

    @Test
    void keepsTitlesWhoseSuffixIsNotASiteName() {
        assertThat(RecipePageParser.withoutSiteName("Lecsó - az igazi", null)).isEqualTo("Lecsó - az igazi");
        assertThat(RecipePageParser.withoutSiteName("Lecsó - nosalty.hu", null)).isEqualTo("Lecsó");
    }
}
