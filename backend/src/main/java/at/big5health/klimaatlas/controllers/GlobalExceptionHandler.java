package at.big5health.klimaatlas.controllers;

import at.big5health.klimaatlas.dtos.ErrorResponse;
import at.big5health.klimaatlas.exceptions.ErrorMessages;
import at.big5health.klimaatlas.exceptions.ExternalApiException;
import at.big5health.klimaatlas.exceptions.InvalidInputException;
import at.big5health.klimaatlas.exceptions.WeatherDataNotFoundException;
import jakarta.validation.ConstraintViolationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

import java.time.LocalDate;
import java.util.stream.Collectors;

@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger LOG = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(ConstraintViolationException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ResponseEntity<ErrorResponse> handleConstraintViolation(ConstraintViolationException e) {
        String specificErrors = e.getConstraintViolations().stream()
                .map(violation -> violation.getPropertyPath() + ": " + violation.getMessage())
                .collect(Collectors.joining("; "));
        String formattedMessage = ErrorMessages.VALIDATION_ERROR.format(specificErrors);
        LOG.warn("Validation failed: {}", formattedMessage);
        return ResponseEntity.badRequest().body(new ErrorResponse(formattedMessage));
    }

    @ExceptionHandler(MissingServletRequestParameterException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ResponseEntity<ErrorResponse> handleMissingParameter(MissingServletRequestParameterException e) {
        String message = ErrorMessages.MISSING_REQUIRED_PARAMETER.format(e.getParameterName());
        LOG.warn("Missing parameter: {}", message);
        return ResponseEntity.badRequest().body(new ErrorResponse(message));
    }

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ResponseEntity<ErrorResponse> handleTypeMismatch(MethodArgumentTypeMismatchException e) {
        String message;
        // Check if the type mismatch was specifically for the LocalDate parameter
        if (e.getRequiredType() != null && e.getRequiredType().equals(LocalDate.class)) {
            message = ErrorMessages.INVALID_DATE_FORMAT.getMessage();
        } else {
            // Generic message for other type mismatches
            message = ErrorMessages.VALIDATION_ERROR.format(
                    String.format("Parameter '%s' should be of type %s", e.getName(), e.getRequiredType() != null ? e.getRequiredType().getSimpleName() : "unknown")
            );
        }
        LOG.warn("Parameter type mismatch: {}", message);
        return ResponseEntity.badRequest().body(new ErrorResponse(message));
    }

    @ExceptionHandler(InvalidInputException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ResponseEntity<ErrorResponse> handleInvalidInput(InvalidInputException e) {
        LOG.warn("Invalid input: {}", e.getMessage());
        return ResponseEntity.badRequest().body(new ErrorResponse(e.getMessage()));
    }

    @ExceptionHandler(WeatherDataNotFoundException.class)
    @ResponseStatus(HttpStatus.NOT_FOUND)
    public ResponseEntity<ErrorResponse> handleWeatherDataNotFound(WeatherDataNotFoundException e) {
        LOG.info("Data not found: {}", e.getMessage());
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(new ErrorResponse(e.getMessage()));
    }

    @ExceptionHandler(ExternalApiException.class)
    @ResponseStatus(HttpStatus.SERVICE_UNAVAILABLE)
    public ResponseEntity<ErrorResponse> handleExternalApiException(ExternalApiException e) {
        LOG.error("External API error: {}", e.getMessage(), e.getCause());
        // Use the standard message from the enum for client response
        return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
                .body(new ErrorResponse(ErrorMessages.EXTERNAL_API_FAILURE.getMessage()));
    }

    @ExceptionHandler(Exception.class) // Catch-all
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    public ResponseEntity<ErrorResponse> handleGenericException(Exception e) {
        LOG.error("An unexpected error occurred: {}", e.getMessage(), e);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(new ErrorResponse(ErrorMessages.UNEXPECTED_ERROR.getMessage()));
    }
}
