package at.big5health.klimaatlas.exceptions;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(value = HttpStatus.BAD_REQUEST)
public class InvalidInputException extends RuntimeException {

    // Constructor taking the enum and optional formatting arguments
    public InvalidInputException(ErrorMessages errorMessages, Object... args) {
        super(errorMessages.format(args));
    }

    // Keep a constructor for direct messages if needed (e.g., from validation)
    public InvalidInputException(String message) {
        super(message);
    }
}
