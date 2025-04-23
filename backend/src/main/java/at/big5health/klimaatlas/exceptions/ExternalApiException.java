package at.big5health.klimaatlas.exceptions;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(value = HttpStatus.SERVICE_UNAVAILABLE) // Default, can be overridden
public class ExternalApiException extends RuntimeException {

    public ExternalApiException(ErrorMessages errorMessages) {
        super(errorMessages.getMessage());
    }

    public ExternalApiException(ErrorMessages errorMessages, Throwable cause) {
        super(errorMessages.getMessage(), cause);
    }

    // Keep constructor for specific messages from API if needed
    public ExternalApiException(String message) {
        super(message);
    }
    public ExternalApiException(String message, Throwable cause) {
        super(message, cause);
    }
}

