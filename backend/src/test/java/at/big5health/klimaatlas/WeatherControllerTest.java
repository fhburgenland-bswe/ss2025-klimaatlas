package at.big5health.klimaatlas;

import at.big5health.klimaatlas.controllers.WeatherController;
import at.big5health.klimaatlas.dtos.Precipitation;
import at.big5health.klimaatlas.dtos.WeatherReportDTO;
import at.big5health.klimaatlas.exceptions.ErrorMessages;
import at.big5health.klimaatlas.exceptions.ExternalApiException;
import at.big5health.klimaatlas.exceptions.WeatherDataNotFoundException;
import at.big5health.klimaatlas.services.WeatherService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import static org.hamcrest.Matchers.is;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;

@WebMvcTest(WeatherController.class) // Load context only for WeatherController
class WeatherControllerTest {

    @Autowired
    private MockMvc mockMvc; // For performing HTTP requests

    @MockitoBean // Create a mock bean for WeatherService in the application context
    private WeatherService weatherService;

    @Autowired
    private ObjectMapper objectMapper; // For JSON assertions if needed

    private final String BASE_URL = "/dailyweather";
    private final String testCity = "Vienna";
    private final Double testLon = 16.3738;
    private final Double testLat = 48.2082;
    private final LocalDate testDate = LocalDate.of(2025, 4, 21);
    private final String testDateStr = testDate.format(DateTimeFormatter.ISO_DATE);

    @Test
    void getWeather_whenValidInputAndDataFound_shouldReturn200Ok() throws Exception {
        // Arrange
        WeatherReportDTO mockReport = new WeatherReportDTO(
                5.5, 15.5, Precipitation.RAIN, 7.1, testLat, testLon, testCity
        );
        given(weatherService.getWeather(testCity, testLon, testLat, testDate)).willReturn(mockReport);

        // Act & Assert
        mockMvc.perform(get(BASE_URL)
                        .param("cityName", testCity)
                        .param("longitude", String.valueOf(testLon))
                        .param("latitude", String.valueOf(testLat))
                        .param("actualDate", testDateStr)
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.minTemp", is(5.5)))
                .andExpect(jsonPath("$.maxTemp", is(15.5)))
                .andExpect(jsonPath("$.precip", is(Precipitation.RAIN.name()))) // Check enum name
                .andExpect(jsonPath("$.sunDuration", is(7.1)))
                .andExpect(jsonPath("$.latitude", is(testLat)))
                .andExpect(jsonPath("$.longitude", is(testLon)));

        verify(weatherService).getWeather(testCity, testLon, testLat, testDate);
    }

    @Test
    void getWeather_whenLatitudeMissing_shouldReturn400BadRequest() throws Exception {
        // Arrange - No service mocking needed as validation happens first

        // Act & Assert
        mockMvc.perform(get(BASE_URL)
                        .param("cityName", testCity)
                        .param("longitude", String.valueOf(testLon))
                        // Missing latitude
                        .param("actualDate", testDateStr)
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").exists()) // Check if error field exists
                .andExpect(jsonPath("$.error", is(ErrorMessages.MISSING_REQUIRED_PARAMETER.format("latitude"))));

        verify(weatherService, never()).getWeather(any(), any(), any(), any());
    }

    @Test
    void getWeather_whenDateFormatInvalid_shouldReturn400BadRequest() throws Exception {
        // Arrange

        // Act & Assert
        mockMvc.perform(get(BASE_URL)
                        .param("cityName", testCity)
                        .param("longitude", String.valueOf(testLon))
                        .param("latitude", String.valueOf(testLat))
                        .param("actualDate", "21-04-2025") // Invalid format
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").exists())
                .andExpect(jsonPath("$.error", is(ErrorMessages.INVALID_DATE_FORMAT.getMessage())));

        verify(weatherService, never()).getWeather(any(), any(), any(), any());
    }

    @Test
    void getWeather_whenServiceThrowsNotFound_shouldReturn404NotFound() throws Exception {
        // Arrange
        given(weatherService.getWeather(testCity, testLon, testLat, testDate))
                .willThrow(new WeatherDataNotFoundException(ErrorMessages.WEATHER_DATA_NOT_FOUND));

        // Act & Assert
        mockMvc.perform(get(BASE_URL)
                        .param("cityName", testCity)
                        .param("longitude", String.valueOf(testLon))
                        .param("latitude", String.valueOf(testLat))
                        .param("actualDate", testDateStr)
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error", is(ErrorMessages.WEATHER_DATA_NOT_FOUND.getMessage())));

        verify(weatherService).getWeather(testCity, testLon, testLat, testDate);
    }

    @Test
    void getWeather_whenServiceThrowsExternalApi_shouldReturn503ServiceUnavailable() throws Exception {
        // Arrange
        given(weatherService.getWeather(testCity, testLon, testLat, testDate))
                .willThrow(new ExternalApiException(ErrorMessages.EXTERNAL_API_FAILURE));

        // Act & Assert
        mockMvc.perform(get(BASE_URL)
                        .param("cityName", testCity)
                        .param("longitude", String.valueOf(testLon))
                        .param("latitude", String.valueOf(testLat))
                        .param("actualDate", testDateStr)
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isServiceUnavailable()) // Matches handler default
                .andExpect(jsonPath("$.error", is(ErrorMessages.EXTERNAL_API_FAILURE.getMessage())));

        verify(weatherService).getWeather(testCity, testLon, testLat, testDate);
    }

    @Test
    void getWeather_whenServiceThrowsUnexpectedError_shouldReturn500InternalServerError() throws Exception {
        // Arrange
        given(weatherService.getWeather(testCity, testLon, testLat, testDate))
                .willThrow(new RuntimeException("Something unexpected broke"));

        // Act & Assert
        mockMvc.perform(get(BASE_URL)
                        .param("cityName", testCity)
                        .param("longitude", String.valueOf(testLon))
                        .param("latitude", String.valueOf(testLat))
                        .param("actualDate", testDateStr)
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.error", is(ErrorMessages.UNEXPECTED_ERROR.getMessage())));

        verify(weatherService).getWeather(testCity, testLon, testLat, testDate);
    }
}
