package at.big5health.klimaatlas.dtos;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

/**
 * Class for representing standardized error responses.
 * <p>
 * This Class is used by the {@link at.big5health.klimaatlas.controllers.GlobalExceptionHandler}
 * to provide a consistent JSON structure for error messages returned to API clients.
 * <p>
 * Lombok annotations {@link Data @Data}, {@link NoArgsConstructor @NoArgsConstructor},
 * and {@link AllArgsConstructor @AllArgsConstructor} are used to generate
 * standard boilerplate code.
 *
 * @see at.big5health.klimaatlas.controllers.GlobalExceptionHandler
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ErrorResponse {

    /**
     * A human-readable message describing the error that occurred.
     * This message is intended for the API client or developer.
     */
    private String error;
}
