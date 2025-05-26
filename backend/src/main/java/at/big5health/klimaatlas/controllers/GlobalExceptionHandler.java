package at.big5health.klimaatlas.controllers;

import at.big5health.klimaatlas.dtos.ErrorResponse;
import at.big5health.klimaatlas.exceptions.ErrorMessages;
import at.big5health.klimaatlas.exceptions.ExternalApiException;
import at.big5health.klimaatlas.exceptions.InvalidInputException;
import at.big5health.klimaatlas.exceptions.WeatherDataNotFoundException;
import at.big5health.klimaatlas.exceptions.CsvParseException;
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
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Global exception handler for the application.
 * <p>
 * This class uses {@link RestControllerAdvice} to provide centralized
 * exception handling across all {@code @RestController} classes. It catches
 * specific exceptions and formats them into a standardized {@link ErrorResponse}
 * object, returning appropriate HTTP status codes.
 *
 * @see ErrorResponse
 * @see ErrorMessages
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    /**
     * Logger instance for this class. Used for logging exception details.
     */
    private static final Logger LOG = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    /**
     * Handles {@link ConstraintViolationException} which occurs when request validation fails.
     * <p>
     * This typically happens due to violations of JSR 380 bean validation annotations
     * (e.g., {@code @NotNull}, {@code @Size}, {@code @Pattern}) on request DTOs or path variables.
     * The response includes a semicolon-separated list of all validation failures.
     *
     * @param e The {@link ConstraintViolationException} instance.
     * @return A {@link ResponseEntity} with HTTP status 400 (Bad Request) and an
     *         {@link ErrorResponse} detailing the validation errors.
     */
    @ExceptionHandler(ConstraintViolationException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST) // Though ResponseEntity sets it, this is good for clarity
    public ResponseEntity<ErrorResponse> handleConstraintViolation(ConstraintViolationException e) {
        String specificErrors = e.getConstraintViolations().stream()
                .map(violation -> violation.getPropertyPath() + ": " + violation.getMessage())
                .collect(Collectors.joining("; "));
        String formattedMessage = ErrorMessages.VALIDATION_ERROR.format(specificErrors);
        LOG.warn("Validation failed: {}", formattedMessage);
        return ResponseEntity.badRequest().body(new ErrorResponse(formattedMessage));
    }

    /**
     * Handles {@link MissingServletRequestParameterException} which occurs when a required
     * request parameter is not provided.
     *
     * @param e The {@link MissingServletRequestParameterException} instance.
     * @return A {@link ResponseEntity} with HTTP status 400 (Bad Request) and an
     *         {@link ErrorResponse} indicating the missing parameter.
     */
    @ExceptionHandler(MissingServletRequestParameterException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ResponseEntity<ErrorResponse> handleMissingParameter(MissingServletRequestParameterException e) {
        String message = ErrorMessages.MISSING_REQUIRED_PARAMETER.format(e.getParameterName());
        LOG.warn("Missing parameter: {}", message);
        return ResponseEntity.badRequest().body(new ErrorResponse(message));
    }

    /**
     * Handles {@link MethodArgumentTypeMismatchException} which occurs when a method
     * argument is not of the expected type.
     * <p>
     * This often happens when a path variable or request parameter cannot be converted
     * to the declared type in the controller method (e.g., providing "abc" for an Integer).
     * A specific message is provided for {@link LocalDate} parsing errors.
     *
     * @param e The {@link MethodArgumentTypeMismatchException} instance.
     * @return A {@link ResponseEntity} with HTTP status 400 (Bad Request) and an
     *         {@link ErrorResponse} detailing the type mismatch.
     */
    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ResponseEntity<ErrorResponse> handleTypeMismatch(MethodArgumentTypeMismatchException e) {
        String message;
        if (e.getRequiredType() != null && e.getRequiredType().equals(LocalDate.class)) {
            message = ErrorMessages.INVALID_DATE_FORMAT.getMessage();
        } else {
            message = ErrorMessages.VALIDATION_ERROR.format(
                    String.format("Parameter '%s' should be of type %s", e.getName(), e.getRequiredType() != null ? e.getRequiredType().getSimpleName() : "unknown")
            );
        }
        LOG.warn("Parameter type mismatch: {}", message);
        return ResponseEntity.badRequest().body(new ErrorResponse(message));
    }

    /**
     * Handles {@link InvalidInputException}, a custom exception indicating that
     * the provided input data is invalid for business logic reasons not covered by
     * standard bean validation.
     *
     * @param e The {@link InvalidInputException} instance.
     * @return A {@link ResponseEntity} with HTTP status 400 (Bad Request) and an
     *         {@link ErrorResponse} containing the exception's message.
     */
    @ExceptionHandler(InvalidInputException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ResponseEntity<ErrorResponse> handleInvalidInput(InvalidInputException e) {
        LOG.warn("Invalid input: {}", e.getMessage());
        return ResponseEntity.badRequest().body(new ErrorResponse(e.getMessage()));
    }

    /**
     * Handles {@link WeatherDataNotFoundException}, a custom exception indicating that
     * requested weather data could not be found.
     *
     * @param e The {@link WeatherDataNotFoundException} instance.
     * @return A {@link ResponseEntity} with HTTP status 404 (Not Found) and an
     *         {@link ErrorResponse} containing the exception's message.
     */
    @ExceptionHandler(WeatherDataNotFoundException.class)
    @ResponseStatus(HttpStatus.NOT_FOUND)
    public ResponseEntity<ErrorResponse> handleWeatherDataNotFound(WeatherDataNotFoundException e) {
        LOG.info("Data not found: {}", e.getMessage()); // Info level as this might be an expected "not found" scenario
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(new ErrorResponse(e.getMessage()));
    }

    /**
     * Handles {@link ExternalApiException}, a custom exception indicating an issue
     * while communicating with an external API.
     * <p>
     * The client receives a generic error message, while the specific cause is logged
     * for internal diagnostics.
     *
     * @param e The {@link ExternalApiException} instance.
     * @return A {@link ResponseEntity} with HTTP status 503 (Service Unavailable) and a
     *         generic {@link ErrorResponse} message.
     */
    @ExceptionHandler(ExternalApiException.class)
    @ResponseStatus(HttpStatus.SERVICE_UNAVAILABLE)
    public ResponseEntity<ErrorResponse> handleExternalApiException(ExternalApiException e) {
        LOG.error("External API error: {}", e.getMessage(), e.getCause());
        return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
                .body(new ErrorResponse(ErrorMessages.EXTERNAL_API_FAILURE.getMessage()));
    }

    /**
     * Handles any other {@link Exception} not specifically caught by other handlers.
     * <p>
     * This acts as a catch-all for unexpected errors, ensuring that the client
     * always receives a standardized JSON error response.
     *
     * @param e The {@link Exception} instance.
     * @return A {@link ResponseEntity} with HTTP status 500 (Internal Server Error) and a
     *         generic {@link ErrorResponse} message.
     */
    @ExceptionHandler(Exception.class)
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    public ResponseEntity<ErrorResponse> handleGenericException(Exception e) {
        LOG.error("An unexpected error occurred: {}", e.getMessage(), e);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(new ErrorResponse(ErrorMessages.UNEXPECTED_ERROR.getMessage()));
    }

    /**
     * Handles {@link CsvParseException} thrown during CSV parsing of population centers.
     * Returns a structured JSON response with status 400 and a list of individual error messages
     * explaining the reason(s) the CSV file could not be processed.
     * Intended for use by the frontend to display detailed validation issues.
     *
     * @param ex the {@link CsvParseException} containing parsing error details
     * @return a {@link ResponseEntity} with HTTP 400 and a JSON body containing the error list
     */
    @ExceptionHandler(CsvParseException.class)
    public ResponseEntity<?> handleCsvParseException(CsvParseException ex) {
        return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body(Map.of(
                        "message", "CSV parsing error",
                        "errors", ex.getErrors()
                ));
    }
}
