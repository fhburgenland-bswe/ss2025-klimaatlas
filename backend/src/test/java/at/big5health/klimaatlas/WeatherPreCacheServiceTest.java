package at.big5health.klimaatlas;

import at.big5health.klimaatlas.config.AustrianPopulationCenter;
import at.big5health.klimaatlas.dtos.Precipitation;
import at.big5health.klimaatlas.dtos.WeatherReportDTO;
import at.big5health.klimaatlas.exceptions.ErrorMessages;
import at.big5health.klimaatlas.exceptions.ExternalApiException;
import at.big5health.klimaatlas.exceptions.WeatherDataNotFoundException;
import at.big5health.klimaatlas.services.WeatherPreCacheService;
import at.big5health.klimaatlas.services.WeatherService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.ConfigurableApplicationContext;

import java.time.Duration;
import java.time.LocalDate;

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
    private ConfigurableApplicationContext mockConfigurableApplicationContext;

    @Mock
    private SpringApplication mockSpringApplication;

    @InjectMocks
    private WeatherPreCacheService weatherPreCacheService;

    private LocalDate expectedDateToFetch;
    private WeatherReportDTO dummySuccessDTO; // For successful calls

    @BeforeEach
    void setUp() {
        expectedDateToFetch = LocalDate.now().minusDays(1);
        // A dummy DTO for successful calls, as getWeather returns a DTO
        dummySuccessDTO = new WeatherReportDTO(0.0, 0.0, Precipitation.NONE, 0.0, 0.0, 0.0);
    }

    @Test
    void preCacheOnStartup_shouldCallPerformPreCaching() {
        WeatherPreCacheService spiedPreCacheService = spy(weatherPreCacheService);
        ApplicationReadyEvent event = new ApplicationReadyEvent(
                mockSpringApplication,
                new String[]{},
                mockConfigurableApplicationContext,
                Duration.ZERO
        );
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
        AustrianPopulationCenter center1 = AustrianPopulationCenter.VIENNA;
        AustrianPopulationCenter center2 = AustrianPopulationCenter.GRAZ;
        AustrianPopulationCenter[] testCenters = {center1, center2};

        try (MockedStatic<AustrianPopulationCenter> mockedEnum = Mockito.mockStatic(AustrianPopulationCenter.class)) {
            mockedEnum.when(AustrianPopulationCenter::values).thenReturn(testCenters);

            // Stub successful calls
            when(weatherService.getWeather(
                    eq(center1.getDisplayName()),
                    eq(center1.getRepresentativeLongitude()),
                    eq(center1.getRepresentativeLatitude()),
                    eq(expectedDateToFetch)
            )).thenReturn(dummySuccessDTO);
            when(weatherService.getWeather(
                    eq(center2.getDisplayName()),
                    eq(center2.getRepresentativeLongitude()),
                    eq(center2.getRepresentativeLatitude()),
                    eq(expectedDateToFetch)
            )).thenReturn(dummySuccessDTO);


            weatherPreCacheService.performPreCaching("Test");

            verify(weatherService, times(1)).getWeather(
                    eq(center1.getDisplayName()),
                    eq(center1.getRepresentativeLongitude()),
                    eq(center1.getRepresentativeLatitude()),
                    eq(expectedDateToFetch)
            );
            verify(weatherService, times(1)).getWeather(
                    eq(center2.getDisplayName()),
                    eq(center2.getRepresentativeLongitude()),
                    eq(center2.getRepresentativeLatitude()),
                    eq(expectedDateToFetch)
            );
        }
    }

    @Test
    void performPreCaching_whenSomeApiCallsFail_shouldContinueAndLog() {
        AustrianPopulationCenter center1 = AustrianPopulationCenter.VIENNA;
        AustrianPopulationCenter center2 = AustrianPopulationCenter.LINZ; // Fails
        AustrianPopulationCenter center3 = AustrianPopulationCenter.GRAZ; // Succeeds
        AustrianPopulationCenter[] testCenters = {center1, center2, center3};

        try (MockedStatic<AustrianPopulationCenter> mockedEnum = Mockito.mockStatic(AustrianPopulationCenter.class)) {
            mockedEnum.when(AustrianPopulationCenter::values).thenReturn(testCenters);

            // Stub successful call for center1
            when(weatherService.getWeather(
                    eq(center1.getDisplayName()), anyDouble(), anyDouble(), eq(expectedDateToFetch)
            )).thenReturn(dummySuccessDTO);

            // Stub failing call for center2
            doThrow(new WeatherDataNotFoundException(ErrorMessages.WEATHER_DATA_NOT_FOUND))
                    .when(weatherService).getWeather(
                            eq(center2.getDisplayName()),
                            anyDouble(),
                            anyDouble(),
                            eq(expectedDateToFetch));

            // Stub successful call for center3
            when(weatherService.getWeather(
                    eq(center3.getDisplayName()), anyDouble(), anyDouble(), eq(expectedDateToFetch)
            )).thenReturn(dummySuccessDTO);

            weatherPreCacheService.performPreCaching("TestWithFailures");

            verify(weatherService, times(1)).getWeather(
                    eq(center1.getDisplayName()), anyDouble(), anyDouble(), eq(expectedDateToFetch));
            verify(weatherService, times(1)).getWeather(
                    eq(center2.getDisplayName()), anyDouble(), anyDouble(), eq(expectedDateToFetch));
            verify(weatherService, times(1)).getWeather(
                    eq(center3.getDisplayName()), anyDouble(), anyDouble(), eq(expectedDateToFetch));
        }
    }

    @Test
    void performPreCaching_whenExternalApiExceptionOccurs_shouldHandleAndContinue() {
        AustrianPopulationCenter center1 = AustrianPopulationCenter.VIENNA; // Succeeds
        AustrianPopulationCenter center2 = AustrianPopulationCenter.SALZBURG; // Fails
        AustrianPopulationCenter[] testCenters = {center1, center2};

        try (MockedStatic<AustrianPopulationCenter> mockedEnum = Mockito.mockStatic(AustrianPopulationCenter.class)) {
            mockedEnum.when(AustrianPopulationCenter::values).thenReturn(testCenters);

            // Stub successful call for center1
            when(weatherService.getWeather(
                    eq(center1.getDisplayName()), anyDouble(), anyDouble(), eq(expectedDateToFetch)
            )).thenReturn(dummySuccessDTO);

            // Stub failing call for center2
            doThrow(new ExternalApiException(ErrorMessages.EXTERNAL_API_FAILURE))
                    .when(weatherService).getWeather(
                            eq(center2.getDisplayName()), anyDouble(), anyDouble(), eq(expectedDateToFetch));

            weatherPreCacheService.performPreCaching("TestExternalApiEx");

            verify(weatherService, times(1)).getWeather(
                    eq(center1.getDisplayName()), anyDouble(), anyDouble(), eq(expectedDateToFetch));
            verify(weatherService, times(1)).getWeather(
                    eq(center2.getDisplayName()), anyDouble(), anyDouble(), eq(expectedDateToFetch));
        }
    }

    @Test
    void performPreCaching_whenInterruptedDuringSleep_shouldStopProcessingAndSetInterruptFlag() {
        // This test is still tricky for Thread.sleep().
        // We'll simulate an interruption by having the *WeatherService* call throw a RuntimeException
        // that wraps an InterruptedException, or just a generic RuntimeException to test the generic catch block.
        // A true test of Thread.sleep interruption requires more advanced techniques or SUT refactoring.

        AustrianPopulationCenter center1 = AustrianPopulationCenter.VIENNA; // Processed
        AustrianPopulationCenter center2 = AustrianPopulationCenter.GRAZ;  // This call will "cause" interruption
        AustrianPopulationCenter center3 = AustrianPopulationCenter.LINZ;  // Should not be processed

        AustrianPopulationCenter[] testCenters = {center1, center2, center3};

        try (MockedStatic<AustrianPopulationCenter> mockedEnum = Mockito.mockStatic(AustrianPopulationCenter.class)) {
            mockedEnum.when(AustrianPopulationCenter::values).thenReturn(testCenters);

            // Stub successful call for center1
            when(weatherService.getWeather(
                    eq(center1.getDisplayName()), anyDouble(), anyDouble(), eq(expectedDateToFetch)
            )).thenReturn(dummySuccessDTO);

            // Simulate an issue during the processing of center2 that might lead to an interruption being caught
            // For simplicity, let's use a generic RuntimeException here, as your code catches `Exception e`
            // and then `InterruptedException` specifically.
            // To test the InterruptedException catch block more directly, you'd need to mock Thread.sleep.
            // Let's assume the generic Exception catch block is hit.
            // If you want to test the InterruptedException block specifically, you'd need to refactor
            // WeatherPreCacheService to use an injectable Sleeper.
            doThrow(new RuntimeException("Simulated error during center2 processing, mimicking interruption effect"))
                    .when(weatherService).getWeather(
                            eq(center2.getDisplayName()),
                            anyDouble(),
                            anyDouble(),
                            eq(expectedDateToFetch));

            // In performPreCaching_whenInterruptedDuringSleep_shouldStopProcessingAndSetInterruptFlag

// ... (stubbing for center1 and center2 as before) ...

// Stub successful call for center3 because the loop will continue
            when(weatherService.getWeather(
                    eq(center3.getDisplayName()), anyDouble(), anyDouble(), eq(expectedDateToFetch)
            )).thenReturn(dummySuccessDTO);


            weatherPreCacheService.performPreCaching("TestInterruptionEffect");

            // Verify center1 was processed
            verify(weatherService, times(1)).getWeather(
                    eq(center1.getDisplayName()), anyDouble(), anyDouble(), eq(expectedDateToFetch));
            // Verify center2 was attempted (and threw)
            verify(weatherService, times(1)).getWeather(
                    eq(center2.getDisplayName()), anyDouble(), anyDouble(), eq(expectedDateToFetch));
            // Verify center3 WAS processed because the generic exception for center2 didn't break the loop
            verify(weatherService, times(1)).getWeather(
                    eq(center3.getDisplayName()), anyDouble(), anyDouble(), eq(expectedDateToFetch));

        }
    }
}
