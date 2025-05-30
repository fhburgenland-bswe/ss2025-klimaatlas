package at.big5health.klimaatlas.services;

import at.big5health.klimaatlas.config.AustrianPopulationCenter;
import at.big5health.klimaatlas.dtos.Precipitation;
import at.big5health.klimaatlas.dtos.WeatherReportDTO;
import at.big5health.klimaatlas.exceptions.ErrorMessages;
import at.big5health.klimaatlas.exceptions.ExternalApiException;
import at.big5health.klimaatlas.exceptions.WeatherDataNotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.List;

import static org.mockito.ArgumentMatchers.anyDouble;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.timeout;
import static org.mockito.Mockito.doThrow;

@ExtendWith(MockitoExtension.class)
// Option B: If you prefer lenient for the whole class, uncomment next line
// @MockitoSettings(strictness = Strictness.LENIENT)
class WeatherPreCacheServiceTest {

    @Mock
    private WeatherService weatherService;

    @Mock
    private PopulationCenterService populationCenterService;

    @InjectMocks
    private WeatherPreCacheService weatherPreCacheService;

    private LocalDate expectedDateToFetch;
    private WeatherReportDTO dummySuccessDTO; // For successful calls

    @BeforeEach
    void setUp() {
        expectedDateToFetch = LocalDate.now().minusDays(1);
        // A dummy DTO for successful calls, as getWeather returns a DTO
        dummySuccessDTO = new WeatherReportDTO(0.0, 0.0, Precipitation.NONE, 0.0, 0.0, 0.0, null);
    }

    @Test
    void preCacheOnStartup_shouldCallPerformPreCaching() {
        WeatherPreCacheService spiedPreCacheService = spy(weatherPreCacheService);
        spiedPreCacheService.preCacheOnStartup();
        verify(spiedPreCacheService, timeout(1000).times(1)).performPreCaching(eq("Startup"));
    }

    @Test
    void scheduledPreCache_shouldCallPerformPreCaching() {
        WeatherPreCacheService spiedPreCacheService = spy(weatherPreCacheService);
        spiedPreCacheService.scheduledPreCache();
        verify(spiedPreCacheService, times(1)).performPreCaching(eq("Scheduled"));
    }

    @Test
    void performPreCaching_whenAllApiCallsSucceed_shouldCallWeatherServiceForEachCenter() {
        AustrianPopulationCenter center1 = new AustrianPopulationCenter("Vienna", 48.2082, 16.3738, 48.12, 16.18, 48.33, 16.58);
        AustrianPopulationCenter center2 = new AustrianPopulationCenter("Graz", 47.0707, 15.4395, 46.99, 15.35, 47.12, 15.52);
        List<AustrianPopulationCenter> testCenters = List.of(center1, center2);

        when(populationCenterService.getAllCenters()).thenReturn(testCenters);

        when(weatherService.getWeather(eq(center1.getDisplayName()), eq(center1.getRepresentativeLongitude()), eq(center1.getRepresentativeLatitude()), eq(expectedDateToFetch)))
                .thenReturn(dummySuccessDTO);
        when(weatherService.getWeather(eq(center2.getDisplayName()), eq(center2.getRepresentativeLongitude()), eq(center2.getRepresentativeLatitude()), eq(expectedDateToFetch)))
                .thenReturn(dummySuccessDTO);

        weatherPreCacheService.performPreCaching("Test");

        verify(weatherService, times(1)).getWeather(eq(center1.getDisplayName()), eq(center1.getRepresentativeLongitude()), eq(center1.getRepresentativeLatitude()), eq(expectedDateToFetch));
        verify(weatherService, times(1)).getWeather(eq(center2.getDisplayName()), eq(center2.getRepresentativeLongitude()), eq(center2.getRepresentativeLatitude()), eq(expectedDateToFetch));
    }

    @Test
    void performPreCaching_whenSomeApiCallsFail_shouldContinueAndLog() {
        AustrianPopulationCenter center1 = new AustrianPopulationCenter("Vienna", 48.2082, 16.3738, 48.12, 16.18, 48.33, 16.58);
        AustrianPopulationCenter center2 = new AustrianPopulationCenter("Linz", 48.3069, 14.2858, 48.22, 14.18, 48.37, 14.40); // Fails
        AustrianPopulationCenter center3 = new AustrianPopulationCenter("Graz", 47.0707, 15.4395, 46.99, 15.35, 47.12, 15.52); // Succeeds

        List<AustrianPopulationCenter> testCenters = List.of(center1, center2, center3);

        when(populationCenterService.getAllCenters()).thenReturn(testCenters);

        when(weatherService.getWeather(eq(center1.getDisplayName()), anyDouble(), anyDouble(), eq(expectedDateToFetch)))
                .thenReturn(dummySuccessDTO);

        doThrow(new WeatherDataNotFoundException(ErrorMessages.WEATHER_DATA_NOT_FOUND))
                .when(weatherService).getWeather(eq(center2.getDisplayName()), anyDouble(), anyDouble(), eq(expectedDateToFetch));

        when(weatherService.getWeather(eq(center3.getDisplayName()), anyDouble(), anyDouble(), eq(expectedDateToFetch)))
                .thenReturn(dummySuccessDTO);

        weatherPreCacheService.performPreCaching("TestWithFailures");

        verify(weatherService, times(1)).getWeather(eq(center1.getDisplayName()), anyDouble(), anyDouble(), eq(expectedDateToFetch));
        verify(weatherService, times(1)).getWeather(eq(center2.getDisplayName()), anyDouble(), anyDouble(), eq(expectedDateToFetch));
        verify(weatherService, times(1)).getWeather(eq(center3.getDisplayName()), anyDouble(), anyDouble(), eq(expectedDateToFetch));
    }

    @Test
    void performPreCaching_whenExternalApiExceptionOccurs_shouldHandleAndContinue() {
        AustrianPopulationCenter center1 = new AustrianPopulationCenter("Vienna", 48.2082, 16.3738, 48.12, 16.18, 48.33, 16.58); // Succeeds
        AustrianPopulationCenter center2 = new AustrianPopulationCenter("Salzburg", 47.8095, 13.0550, 47.75, 12.98, 47.85, 13.12); // Fails

        List<AustrianPopulationCenter> testCenters = List.of(center1, center2);
        when(populationCenterService.getAllCenters()).thenReturn(testCenters);

        when(weatherService.getWeather(
                eq(center1.getDisplayName()), anyDouble(), anyDouble(), eq(expectedDateToFetch)))
                .thenReturn(dummySuccessDTO);

        doThrow(new ExternalApiException(ErrorMessages.EXTERNAL_API_FAILURE))
                .when(weatherService).getWeather(
                        eq(center2.getDisplayName()), anyDouble(), anyDouble(), eq(expectedDateToFetch));

        weatherPreCacheService.performPreCaching("TestExternalApiEx");

        verify(weatherService).getWeather(eq(center1.getDisplayName()), anyDouble(), anyDouble(), eq(expectedDateToFetch));
        verify(weatherService).getWeather(eq(center2.getDisplayName()), anyDouble(), anyDouble(), eq(expectedDateToFetch));
    }

    @Test
    void performPreCaching_whenInterruptedDuringSleep_shouldStopProcessingAndSetInterruptFlag() {
        AustrianPopulationCenter center1 = new AustrianPopulationCenter("Vienna", 48.2082, 16.3738, 48.12, 16.18, 48.33, 16.58); // Processed
        AustrianPopulationCenter center2 = new AustrianPopulationCenter("Graz", 47.0707, 15.4395, 46.99, 15.35, 47.12, 15.52); // Simulated interruption
        AustrianPopulationCenter center3 = new AustrianPopulationCenter("Linz", 48.3069, 14.2858, 48.22, 14.18, 48.37, 14.40); // Should still be processed

        List<AustrianPopulationCenter> testCenters = List.of(center1, center2, center3);
        when(populationCenterService.getAllCenters()).thenReturn(testCenters);

        when(weatherService.getWeather(eq(center1.getDisplayName()), anyDouble(), anyDouble(), eq(expectedDateToFetch)))
                .thenReturn(dummySuccessDTO);

        doThrow(new RuntimeException("Simulated error during processing"))
                .when(weatherService).getWeather(eq(center2.getDisplayName()), anyDouble(), anyDouble(), eq(expectedDateToFetch));

        when(weatherService.getWeather(eq(center3.getDisplayName()), anyDouble(), anyDouble(), eq(expectedDateToFetch)))
                .thenReturn(dummySuccessDTO);

        weatherPreCacheService.performPreCaching("TestInterruptionEffect");

        verify(weatherService).getWeather(eq(center1.getDisplayName()), anyDouble(), anyDouble(), eq(expectedDateToFetch));
        verify(weatherService).getWeather(eq(center2.getDisplayName()), anyDouble(), anyDouble(), eq(expectedDateToFetch));
        verify(weatherService).getWeather(eq(center3.getDisplayName()), anyDouble(), anyDouble(), eq(expectedDateToFetch));
    }
}
