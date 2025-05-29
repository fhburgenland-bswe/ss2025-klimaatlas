package at.big5health.klimaatlas.controllers;

import at.big5health.klimaatlas.config.AustrianPopulationCenter;
import at.big5health.klimaatlas.dtos.Precipitation;
import at.big5health.klimaatlas.dtos.WeatherReportDTO;
import at.big5health.klimaatlas.exceptions.ErrorMessages;
import at.big5health.klimaatlas.exceptions.ExternalApiException;
import at.big5health.klimaatlas.exceptions.WeatherDataNotFoundException;
import at.big5health.klimaatlas.services.PopulationCenterService;
import at.big5health.klimaatlas.services.WeatherService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import static org.hamcrest.Matchers.is;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.mock;

import java.util.List;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;

@WebMvcTest(WeatherController.class) // Load context only for WeatherController
class WeatherControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private WeatherService weatherService;

    @MockBean
    private CacheManager cacheManager;

    @MockBean
    private PopulationCenterService populationCenterService;

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

    @Test
    void getAllCachedWeatherData_whenAllEntriesAreCached_shouldReturn200WithData() throws Exception {

        LocalDate date = testDate;

        AustrianPopulationCenter center1 = new AustrianPopulationCenter(
                "Vienna (Wien)", 48.2082, 16.3738, 48.1200, 16.1800, 48.3300, 16.5800);
        AustrianPopulationCenter center2 = new AustrianPopulationCenter(
                "Graz", 47.0707, 15.4395, 46.9900, 15.3500, 47.1200, 15.5200);

        given(populationCenterService.getAllCenters()).willReturn(List.of(center1, center2));

        Cache mockCache = mock(Cache.class);
        given(cacheManager.getCache("weatherCache")).willReturn(mockCache);

        String key1 = "48.2082_16.3738_" + date;
        String key2 = "47.0707_15.4395_" + date;

        WeatherReportDTO dto1 = new WeatherReportDTO(5.0, 15.0, Precipitation.DRIZZLE, 3600.0, 48.2082, 16.3738, null);
        WeatherReportDTO dto2 = new WeatherReportDTO(6.0, 18.0, Precipitation.NONE, 4000.0, 47.0707, 15.4395, null);

        given(mockCache.get(key1, WeatherReportDTO.class)).willReturn(dto1);
        given(mockCache.get(key2, WeatherReportDTO.class)).willReturn(dto2);

        mockMvc.perform(get(BASE_URL + "/cached")
                        .param("actualDate", testDateStr)
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].cityName", is("Vienna (Wien)")))
                .andExpect(jsonPath("$[1].cityName", is("Graz")));
    }

    @Test
    void getAllCachedWeatherData_whenCacheIsNull_shouldReturn500() throws Exception {
        given(cacheManager.getCache("weatherCache")).willReturn(null);

        mockMvc.perform(get(BASE_URL + "/cached")
                        .param("actualDate", testDateStr)
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isInternalServerError());
    }

    @Test
    void getAllCachedWeatherData_whenOneEntryIsMissing_shouldReturn204() throws Exception {

        LocalDate date = testDate;

        AustrianPopulationCenter center1 = new AustrianPopulationCenter(
                "Vienna (Wien)", 48.2082, 16.3738, 48.1200, 16.1800, 48.3300, 16.5800);
        AustrianPopulationCenter center2 = new AustrianPopulationCenter(
                "Linz", 48.3069, 14.2858, 48.2200, 14.1800, 48.3700, 14.4000);

        given(populationCenterService.getAllCenters()).willReturn(List.of(center1, center2));

        Cache mockCache = mock(Cache.class);
        given(cacheManager.getCache("weatherCache")).willReturn(mockCache);

        given(mockCache.get("48.2_16.3_" + date, WeatherReportDTO.class)).willReturn(
                new WeatherReportDTO(5.0, 10.0, Precipitation.RAIN, 3000.0, 48.2, 16.3, null)
        );
        given(mockCache.get("48.3_14.3_" + date, WeatherReportDTO.class)).willReturn(null); // Missing entry

        mockMvc.perform(get(BASE_URL + "/cached")
                        .param("actualDate", testDateStr)
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isNoContent());
    }
}
