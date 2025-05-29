package at.big5health.klimaatlas.controllers;

import at.big5health.klimaatlas.dtos.ErrorResponse;
import at.big5health.klimaatlas.exceptions.WeatherDataNotFoundException;
import at.big5health.klimaatlas.exceptions.InvalidInputException;
import at.big5health.klimaatlas.exceptions.ExternalApiException;
import at.big5health.klimaatlas.exceptions.ErrorMessages;
import at.big5health.klimaatlas.exceptions.CsvParseException;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;
import jakarta.validation.Path;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class GlobalExceptionHandlerTest {

    private GlobalExceptionHandler handler;

    @BeforeEach
    void setUp() {
        handler = new GlobalExceptionHandler();
    }

    @Test
    void handleConstraintViolation_shouldReturnBadRequestWithMessage() {
        ConstraintViolation<?> violation = mock(ConstraintViolation.class);
        Path mockPath = mock(Path.class);
        when(mockPath.toString()).thenReturn("param");
        when(violation.getPropertyPath()).thenReturn(mockPath);
        when(violation.getMessage()).thenReturn("must not be null");

        ConstraintViolationException exception = new ConstraintViolationException(Set.of(violation));
        ResponseEntity<ErrorResponse> response = handler.handleConstraintViolation(exception);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(Objects.requireNonNull(response.getBody()).getError()).contains("param: must not be null");
    }

    @Test
    void handleMissingParameter_shouldReturnBadRequestWithMessage() {
        MissingServletRequestParameterException ex = new MissingServletRequestParameterException("date", "String");
        ResponseEntity<ErrorResponse> response = handler.handleMissingParameter(ex);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(Objects.requireNonNull(response.getBody()).getError()).contains("Missing required parameter: date");
    }

    @Test
    void handleTypeMismatch_shouldReturnBadRequestWithDateFormatMessage() {
        MethodArgumentTypeMismatchException ex = new MethodArgumentTypeMismatchException(
                "abc", String.class, "actualDate", null, new IllegalArgumentException()
        );
        ResponseEntity<ErrorResponse> response = handler.handleTypeMismatch(ex);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(Objects.requireNonNull(response.getBody()).getError()).contains("Parameter 'actualDate' should be of type String");
    }

    @Test
    void handleInvalidInput_shouldReturnBadRequest() {
        InvalidInputException ex = new InvalidInputException("Invalid city code");
        ResponseEntity<ErrorResponse> response = handler.handleInvalidInput(ex);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(Objects.requireNonNull(response.getBody()).getError()).isEqualTo("Invalid city code");
    }

    @Test
    void handleNotFound_shouldReturn404() {
        WeatherDataNotFoundException ex = new WeatherDataNotFoundException(ErrorMessages.WEATHER_DATA_NOT_FOUND);
        ResponseEntity<ErrorResponse> response = handler.handleWeatherDataNotFound(ex);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(Objects.requireNonNull(response.getBody()).getError()).isEqualTo("Weather data not found for the specified location and date.");
    }

    @Test
    void handleExternalApi_shouldReturn503() {
        ExternalApiException ex = new ExternalApiException("timeout");
        ResponseEntity<ErrorResponse> response = handler.handleExternalApiException(ex);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.SERVICE_UNAVAILABLE);
        assertThat(Objects.requireNonNull(response.getBody()).getError()).contains("Failed to retrieve weather data from the external service.");
    }

    @Test
    void handleGenericException_shouldReturn500() {
        Exception ex = new Exception("Something broke");
        ResponseEntity<ErrorResponse> response = handler.handleGenericException(ex);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
        assertThat(Objects.requireNonNull(response.getBody()).getError()).contains("An internal server error occurred.");
    }

    @Test
    void handleCsvParseException_shouldReturn400WithList() {
        CsvParseException ex = new CsvParseException(List.of("line 1: invalid", "line 2: missing value"));
        ResponseEntity<?> response = handler.handleCsvParseException(ex);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody()).isInstanceOf(Map.class);
        Map<?, ?> body = (Map<?, ?>) response.getBody();
        assertThat(body.get("message")).isEqualTo("CSV parsing error");

        List<String> errors = (List<String>) body.get("errors");
        assertThat(errors).contains("line 1: invalid", "line 2: missing value");
    }

    @Test
    void handleTypeMismatch_withLocalDate_shouldReturnDateFormatMessage() {
        MethodArgumentTypeMismatchException ex = new MethodArgumentTypeMismatchException(
                "invalid-date", LocalDate.class, "date", null, new IllegalArgumentException()
        );

        ResponseEntity<ErrorResponse> response = handler.handleTypeMismatch(ex);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(Objects.requireNonNull(response.getBody()).getError())
                .isEqualTo(ErrorMessages.INVALID_DATE_FORMAT.getMessage());
    }

    @Test
    void handleTypeMismatch_withNullType_shouldReturnUnknownTypeMessage() {
        MethodArgumentTypeMismatchException ex = new MethodArgumentTypeMismatchException(
                "abc", null, "param", null, new IllegalArgumentException()
        );

        ResponseEntity<ErrorResponse> response = handler.handleTypeMismatch(ex);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(Objects.requireNonNull(response.getBody()).getError())
                .contains("Parameter 'param' should be of type unknown");
    }
}