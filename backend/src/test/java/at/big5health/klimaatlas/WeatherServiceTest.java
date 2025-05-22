package at.big5health.klimaatlas;

import at.big5health.klimaatlas.httpclients.ExternalWeatherApiClient;
import at.big5health.klimaatlas.dtos.Precipitation;
import at.big5health.klimaatlas.dtos.WeatherReportDTO;
import at.big5health.klimaatlas.dtos.spartacus.SpartacusParameter;
import at.big5health.klimaatlas.dtos.spartacus.SpartacusFeature;
import at.big5health.klimaatlas.dtos.spartacus.SpartacusGeometry;
import at.big5health.klimaatlas.dtos.spartacus.SpartacusFeatureCollection;
import at.big5health.klimaatlas.dtos.spartacus.SpartacusProperties;
import at.big5health.klimaatlas.exceptions.ErrorMessages;
import at.big5health.klimaatlas.exceptions.ExternalApiException;
import at.big5health.klimaatlas.exceptions.WeatherDataNotFoundException;
import at.big5health.klimaatlas.grid.BoundingBox;
import at.big5health.klimaatlas.grid.GridCellInfo;
import at.big5health.klimaatlas.grid.GridUtil;
import at.big5health.klimaatlas.services.WeatherService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import reactor.core.publisher.Mono;

import java.time.LocalDate;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.anyDouble;
import static org.mockito.Mockito.doThrow;


@ExtendWith(MockitoExtension.class)
class WeatherServiceTest {

    // Mocks for dependencies injected into the spy
    @Mock
    private ExternalWeatherApiClient externalClient;
    @Mock
    private GridUtil gridUtil;

    // Spy on the actual WeatherService instance
    // Mocks above will be injected into this instance
    @Spy
    @InjectMocks
    private WeatherService weatherService;

    // Test data remains the same
    private double testLat;
    private double testLon;
    private LocalDate testDate;
    private String testCity;
    private GridCellInfo testGridCellInfo;
    private BoundingBox testBbox;
    private String testCellId;
    private double targetLat;
    private double targetLon;

    @BeforeEach
    void setUp() {
        testLat = 48.2082;
        testLon = 16.3738;
        testDate = LocalDate.of(2025, 4, 21);
        testCity = "Vienna";
        testCellId = "cell_48.208200_16.373800";
        targetLat = 48.2082;
        targetLon = 16.3738;
        testBbox = new BoundingBox(48.207, 16.372, 48.209, 16.375);
        testGridCellInfo = new GridCellInfo(testCellId, testBbox, targetLat, targetLon);

        // --- REMOVED common mocking from here ---
        // given(gridUtil.getGridCellForCoordinates(testLat, testLon)).willReturn(testGridCellInfo);
    }

    // --- Tests for getWeather() method ---

    @Test
    void getWeather_whenDataFound_shouldReturnDTOWithOriginalCoords() {
        // Arrange
        given(gridUtil.getGridCellForCoordinates(testLat, testLon)).willReturn(testGridCellInfo);

        // Ensure this DTO matches the structure including sunDuration
        WeatherReportDTO fetchedDtoFromInternalCall = new WeatherReportDTO(
                5.0, 15.0, Precipitation.DRIZZLE, 3600.0, // Example sun duration in seconds
                null, null, null // Internal DTO has null lat/lon before getWeather sets them
        );
        doReturn(Optional.of(fetchedDtoFromInternalCall))
                .when(weatherService).getOrFetchGridCellData(testCellId, testBbox, testDate, targetLat, targetLon);

        // Act
        WeatherReportDTO result = weatherService.getWeather(testCity, testLon, testLat, testDate);

        // Assert
        assertThat(result).isNotNull();
        assertThat(result.getMinTemp()).isEqualTo(5.0);
        assertThat(result.getMaxTemp()).isEqualTo(15.0);
        assertThat(result.getPrecip()).isEqualTo(Precipitation.DRIZZLE);
        assertThat(result.getSunDuration()).isEqualTo(3600.0); // Assert sun duration
        assertThat(result.getLatitude()).isEqualTo(testLat);
        assertThat(result.getLongitude()).isEqualTo(testLon);

        verify(gridUtil).getGridCellForCoordinates(testLat, testLon);
        verify(weatherService).getOrFetchGridCellData(testCellId, testBbox, testDate, targetLat, targetLon);
        verify(externalClient, never()).fetchGridData(any(), any());
    }


    @Test
    void getWeather_whenGridUtilFails_shouldThrowException() {
        // Arrange
        // Mock gridUtil for *this test* to throw an exception
        given(gridUtil.getGridCellForCoordinates(testLat, testLon)).willThrow(new RuntimeException("Grid calculation failed"));

        // Act & Assert
        assertThatThrownBy(() -> weatherService.getWeather(testCity, testLon, testLat, testDate))
                .isInstanceOf(ExternalApiException.class)
                .hasMessageContaining(ErrorMessages.GRID_UTIL_ERROR.getMessage());

        verify(gridUtil).getGridCellForCoordinates(testLat, testLon);
        // Verify the internal fetching method was never called because gridUtil failed first
        verify(weatherService, never()).getOrFetchGridCellData(any(), any(), any(), anyDouble(), anyDouble());
        verify(externalClient, never()).fetchGridData(any(), any());
    }

    @Test
    void getWeather_whenDataNotFound_shouldThrowNotFoundException() {
        // Arrange
        // Mock gridUtil for *this test*
        given(gridUtil.getGridCellForCoordinates(testLat, testLon)).willReturn(testGridCellInfo);
        // Stub the *internal* call to return empty Optional
        doReturn(Optional.empty())
                .when(weatherService).getOrFetchGridCellData(testCellId, testBbox, testDate, targetLat, targetLon);

        // Act & Assert
        assertThatThrownBy(() -> weatherService.getWeather(testCity, testLon, testLat, testDate))
                .isInstanceOf(WeatherDataNotFoundException.class)
                .hasMessage(ErrorMessages.WEATHER_DATA_NOT_FOUND.getMessage());

        verify(gridUtil).getGridCellForCoordinates(testLat, testLon);
        verify(weatherService).getOrFetchGridCellData(testCellId, testBbox, testDate, targetLat, targetLon);
        verify(externalClient, never()).fetchGridData(any(), any());
    }

    @Test
    void getWeather_whenApiClientFailsInternally_shouldThrowExternalApiException() {
        // Arrange
        // Mock gridUtil for *this test*
        given(gridUtil.getGridCellForCoordinates(testLat, testLon)).willReturn(testGridCellInfo);
        // Stub the *internal* call to throw the exception
        doThrow(new ExternalApiException(ErrorMessages.EXTERNAL_API_FAILURE))
                .when(weatherService).getOrFetchGridCellData(testCellId, testBbox, testDate, targetLat, targetLon);

        // Act & Assert
        assertThatThrownBy(() -> weatherService.getWeather(testCity, testLon, testLat, testDate))
                .isInstanceOf(ExternalApiException.class)
                .hasMessage(ErrorMessages.EXTERNAL_API_FAILURE.getMessage());

        verify(gridUtil).getGridCellForCoordinates(testLat, testLon);
        verify(weatherService).getOrFetchGridCellData(testCellId, testBbox, testDate, targetLat, targetLon);
        verify(externalClient, never()).fetchGridData(any(), any());
    }

    @Test
    void getOrFetchGridCellData_whenApiClientSucceeds_shouldReturnOptionalDTO() {
        // Arrange
        double expectedSunDuration = 7200.0; // 2 hours in seconds
        SpartacusFeatureCollection mockCollection = createMockFeatureCollection(
                targetLon, targetLat, 6.3, 12.9, 0.2, expectedSunDuration // Pass sun duration
        );
        given(externalClient.fetchGridData(testBbox, testDate)).willReturn(Mono.just(mockCollection));

        // Act
        Optional<WeatherReportDTO> result = weatherService.getOrFetchGridCellData(testCellId, testBbox, testDate, targetLat, targetLon);

        // Assert
        assertThat(result).isPresent();
        WeatherReportDTO dto = result.get();
        assertThat(dto.getMinTemp()).isEqualTo(6.3);
        assertThat(dto.getMaxTemp()).isEqualTo(12.9);
        assertThat(dto.getPrecip()).isEqualTo(Precipitation.DRIZZLE);
        assertThat(dto.getSunDuration()).isEqualTo(expectedSunDuration); // Assert sun duration
        // DTO from getOrFetchGridCellData has null lat/lon before getWeather sets them
        assertThat(dto.getLatitude()).isNull();
        assertThat(dto.getLongitude()).isNull();

        verify(externalClient).fetchGridData(testBbox, testDate);
        verify(gridUtil, never()).getGridCellForCoordinates(anyDouble(), anyDouble());
    }

    @Test
    void getOrFetchGridCellData_whenApiClientReturnsMultipleFeatures_shouldFindClosest() {
        // Arrange
        double sunDurationFar = 3600.0;
        double sunDurationClose = 7200.0;
        SpartacusFeature farFeature = createSingleMockFeature(
                targetLon + 0.1, targetLat + 0.1, 5.0, 10.0, 0.0, sunDurationFar
        );
        SpartacusFeature closeFeature = createSingleMockFeature(
                targetLon + 0.0001, targetLat - 0.0001, 6.3, 12.9, 0.2, sunDurationClose
        );
        SpartacusFeatureCollection mockCollection = new SpartacusFeatureCollection();
        mockCollection.setFeatures(List.of(farFeature, closeFeature));
        given(externalClient.fetchGridData(testBbox, testDate)).willReturn(Mono.just(mockCollection));

        // Act
        Optional<WeatherReportDTO> result = weatherService.getOrFetchGridCellData(testCellId, testBbox, testDate, targetLat, targetLon);

        // Assert
        assertThat(result).isPresent();
        WeatherReportDTO dto = result.get();
        assertThat(dto.getMinTemp()).isEqualTo(6.3); // Data from closeFeature
        assertThat(dto.getMaxTemp()).isEqualTo(12.9);
        assertThat(dto.getPrecip()).isEqualTo(Precipitation.DRIZZLE);
        assertThat(dto.getSunDuration()).isEqualTo(sunDurationClose); // Assert sun duration from closeFeature
        assertThat(dto.getLatitude()).isNull();
        assertThat(dto.getLongitude()).isNull();

        verify(externalClient).fetchGridData(testBbox, testDate);
        verify(gridUtil, never()).getGridCellForCoordinates(anyDouble(), anyDouble());
    }

    @Test
    void getOrFetchGridCellData_whenApiClientReturnsEmptyFeatures_shouldReturnEmptyOptional() {
        // Arrange
        SpartacusFeatureCollection mockCollection = new SpartacusFeatureCollection();
        mockCollection.setFeatures(Collections.emptyList());
        given(externalClient.fetchGridData(testBbox, testDate)).willReturn(Mono.just(mockCollection));

        // Act
        Optional<WeatherReportDTO> result = weatherService.getOrFetchGridCellData(testCellId, testBbox, testDate, targetLat, targetLon);

        // Assert
        assertThat(result).isNotPresent();
        verify(externalClient).fetchGridData(testBbox, testDate);
        verify(gridUtil, never()).getGridCellForCoordinates(anyDouble(), anyDouble());
    }

    @Test
    void getOrFetchGridCellData_whenApiClientFails_shouldThrowExternalApiException() {
        // Arrange
        given(externalClient.fetchGridData(testBbox, testDate))
                .willReturn(Mono.error(new ExternalApiException(ErrorMessages.EXTERNAL_API_FAILURE)));

        // Act & Assert
        assertThatThrownBy(() -> weatherService.getOrFetchGridCellData(testCellId, testBbox, testDate, targetLat, targetLon))
                .isInstanceOf(ExternalApiException.class)
                .hasMessage(ErrorMessages.EXTERNAL_API_FAILURE.getMessage());

        verify(externalClient).fetchGridData(testBbox, testDate);
        verify(gridUtil, never()).getGridCellForCoordinates(anyDouble(), anyDouble());
    }

    // Helper methods
    private SpartacusFeatureCollection createMockFeatureCollection(
            double lon, double lat, double minT, double maxT, double precip, Double sunDuration // Add sunDuration
    ) {
        SpartacusFeatureCollection collection = new SpartacusFeatureCollection();
        collection.setFeatures(List.of(createSingleMockFeature(lon, lat, minT, maxT, precip, sunDuration)));
        return collection;
    }

    private SpartacusFeature createSingleMockFeature(
            double lon, double lat, double minT, double maxT, double precip, Double sunDuration // Add sunDuration
    ) {
        SpartacusFeature feature = new SpartacusFeature();
        SpartacusGeometry geometry = new SpartacusGeometry();
        geometry.setCoordinates(List.of(lon, lat)); // Assuming lon is first, lat is second
        feature.setGeometry(geometry);

        SpartacusProperties properties = new SpartacusProperties();
        Map<String, SpartacusParameter> parametersMap = new java.util.HashMap<>();

        SpartacusParameter tnParam = new SpartacusParameter();
        tnParam.setData(List.of(minT));
        parametersMap.put("TN", tnParam);

        SpartacusParameter txParam = new SpartacusParameter();
        txParam.setData(List.of(maxT));
        parametersMap.put("TX", txParam);

        SpartacusParameter rrParam = new SpartacusParameter();
        rrParam.setData(List.of(precip));
        parametersMap.put("RR", rrParam);

        // Add SA parameter if sunDuration is provided
        if (sunDuration != null) {
            SpartacusParameter saParam = new SpartacusParameter();
            saParam.setData(List.of(sunDuration));
            parametersMap.put("SA", saParam);
        }

        properties.setParameters(parametersMap);
        feature.setProperties(properties);
        return feature;
    }

}
