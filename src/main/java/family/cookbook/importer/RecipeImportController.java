package family.cookbook.importer;

import family.cookbook.importer.dto.ImportRequest;
import family.cookbook.importer.dto.ImportedRecipe;
import family.cookbook.translation.Languages;
import jakarta.validation.Valid;
import org.springframework.http.HttpHeaders;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/recipes/import")
public class RecipeImportController {

    private final RecipeImportService importService;

    public RecipeImportController(RecipeImportService importService) {
        this.importService = importService;
    }

    // Reads a recipe from a web page and answers with a draft for the recipe form. The photo is
    // saved into the images folder; the recipe itself is only saved when the form is submitted.
    @PostMapping
    public ImportedRecipe importRecipe(@Valid @RequestBody ImportRequest request,
                                       @RequestHeader(value = HttpHeaders.ACCEPT_LANGUAGE, required = false)
                                       String acceptLanguage) {
        return importService.importFrom(request.url(), Languages.fromHeader(acceptLanguage));
    }
}
