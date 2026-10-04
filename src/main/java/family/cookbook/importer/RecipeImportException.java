package family.cookbook.importer;

import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.ErrorResponseException;

// An import that couldn't be done. Besides the usual problem detail, the response carries a `code`
// (e.g. BLOCKED, TIMEOUT) so the website can explain it in the visitor's language.
public class RecipeImportException extends ErrorResponseException {

    private final String code;

    public RecipeImportException(HttpStatus status, String code, String detail) {
        super(status, problem(status, code, detail), null);
        this.code = code;
    }

    private static ProblemDetail problem(HttpStatus status, String code, String detail) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(status, detail);
        problem.setProperty("code", code);
        return problem;
    }

    public String getCode() {
        return code;
    }
}
