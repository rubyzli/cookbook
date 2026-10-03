package family.cookbook;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class ApiExceptionHandler {

    // Backstop for the unique constraint on name when two creates race past the exists check
    @ExceptionHandler(DataIntegrityViolationException.class)
    public ProblemDetail handleConstraintViolation(DataIntegrityViolationException ex) {
        return ProblemDetail.forStatusAndDetail(HttpStatus.CONFLICT, "Resource conflicts with existing data");
    }
}
