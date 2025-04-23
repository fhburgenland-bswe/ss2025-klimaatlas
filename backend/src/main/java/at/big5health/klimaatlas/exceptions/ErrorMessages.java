package at.big5health.klimaatlas.exceptions;

import lombok.Getter;

@Getter
public enum ErrorMessages {

    // Validation / Input Errors (400)
    VALIDATION_ERROR("Invalid request parameters: %s"), // Placeholder for specific details
    MISSING_REQUIRED_PARAMETER("Missing required parameter: %s"),
    INVALID_DATE_FORMAT("Invalid date format. Please use YYYY-MM-DD."),

    // Data Not Found Errors (404)
    WEATHER_DATA_NOT_FOUND("Weather data not found for the specified location and date."),

    // External Service Errors (5xx - often 503 or 502)
    EXTERNAL_API_FAILURE("Failed to retrieve weather data from the external service."),
    EXTERNAL_API_TIMEOUT("External weather service timed out."),

    // Internal Server Errors (500)
    UNEXPECTED_ERROR("An internal server error occurred."),
    CACHE_ERROR("An error occurred while accessing the cache."),
    GRID_UTIL_ERROR("An error occurred during grid cell calculation.");


    private final String message;

    ErrorMessages(String message) {
        this.message = message;
    }

    public String format(Object... args) {
        return String.format(this.message, args);
    }
}
