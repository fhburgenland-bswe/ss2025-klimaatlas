package at.big5health.klimaatlas;

import at.big5health.klimaatlas.grid.GridCellInfo;
import at.big5health.klimaatlas.grid.GridUtil;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import java.util.Locale;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within; // Use AssertJ's within for Offset

class GridUtilTest {

    private GridUtil gridUtil;

    // Constants from GridUtil for test calculations
    private static final double METERS_PER_DEGREE_LATITUDE = 111132.954;
    private static final double METERS_PER_DEGREE_LONGITUDE_AT_EQUATOR = 111319.488;
    private static final double CELL_SIZE_METERS = 1000.0;
    private static final double BUFFER_FACTOR = 1.1; // Buffer used in GridUtil

    @BeforeEach
    void setUp() {
        gridUtil = new GridUtil();
    }

    @ParameterizedTest // Use parameterized test for different locations
    @CsvSource({
            "48.2082, 16.3738", // Vienna (Example)
            "47.2692, 11.4041", // Innsbruck (Different Latitude)
            "46.6247, 14.3050", // Klagenfurt (Southern Austria)
            "48.3064, 14.2858"  // Linz (Different Longitude)
    })
    void getGridCellForCoordinates_shouldCalculateCorrectly(double inputLat, double inputLon) {
        // Arrange: Calculate expected values based on GridUtil's logic

        // 1. Expected spacing at input latitude
        double latitudeRadians = Math.toRadians(inputLat);
        double metersPerDegreeLongitude = METERS_PER_DEGREE_LONGITUDE_AT_EQUATOR * Math.cos(latitudeRadians);
        if (metersPerDegreeLongitude < 1.0) metersPerDegreeLongitude = 1.0; // Safety check

        double latSpacingDegrees = CELL_SIZE_METERS / METERS_PER_DEGREE_LATITUDE;
        double lonSpacingDegrees = CELL_SIZE_METERS / metersPerDegreeLongitude;

        // 2. Expected snapped center coordinates
        double expectedSnappedCenterLat = Math.round(inputLat / latSpacingDegrees) * latSpacingDegrees;
        double expectedSnappedCenterLon = Math.round(inputLon / lonSpacingDegrees) * lonSpacingDegrees;

        // 3. Expected Cell ID
        String expectedCellId = String.format(Locale.US, "cell_%.6f_%.6f", expectedSnappedCenterLat, expectedSnappedCenterLon);

        // 4. Expected Bbox corners (using spacing at the *snapped* center)
        double centerLatRadians = Math.toRadians(expectedSnappedCenterLat);
        double metersPerDegLonAtCenter = METERS_PER_DEGREE_LONGITUDE_AT_EQUATOR * Math.cos(centerLatRadians);
        if (metersPerDegLonAtCenter < 1.0) metersPerDegLonAtCenter = 1.0;

        double halfLatSpacing = (CELL_SIZE_METERS / 2.0) / METERS_PER_DEGREE_LATITUDE;
        double halfLonSpacing = (CELL_SIZE_METERS / 2.0) / metersPerDegLonAtCenter;

        double bufferedHalfLat = halfLatSpacing * BUFFER_FACTOR;
        double bufferedHalfLon = halfLonSpacing * BUFFER_FACTOR;

        double expectedMinLat = expectedSnappedCenterLat - bufferedHalfLat;
        double expectedMaxLat = expectedSnappedCenterLat + bufferedHalfLat;
        double expectedMinLon = expectedSnappedCenterLon - bufferedHalfLon;
        double expectedMaxLon = expectedSnappedCenterLon + bufferedHalfLon;

        // Act
        GridCellInfo result = gridUtil.getGridCellForCoordinates(inputLat, inputLon);

        // Assert
        assertThat(result).isNotNull();
        // Use AssertJ's within() for floating-point comparisons with a small tolerance
        org.assertj.core.data.Offset<Double> tolerance = within(0.000001);

        assertThat(result.getTargetLatitude()).isEqualTo(expectedSnappedCenterLat, tolerance);
        assertThat(result.getTargetLongitude()).isEqualTo(expectedSnappedCenterLon, tolerance);
        assertThat(result.getCellId()).isEqualTo(expectedCellId);

        assertThat(result.getBbox()).isNotNull();
        assertThat(result.getBbox().getMinLat()).isEqualTo(expectedMinLat, tolerance);
        assertThat(result.getBbox().getMaxLat()).isEqualTo(expectedMaxLat, tolerance);
        assertThat(result.getBbox().getMinLon()).isEqualTo(expectedMinLon, tolerance);
        assertThat(result.getBbox().getMaxLon()).isEqualTo(expectedMaxLon, tolerance);
    }
}
