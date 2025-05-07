package at.big5health.klimaatlas.grid;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.*;

@Component
public class GridUtil {

    // Constants for calculation (Approximate values)
    private static final double METERS_PER_DEGREE_LATITUDE = 111132.954;
    // Meters per degree longitude depends on latitude
    private static final double METERS_PER_DEGREE_LONGITUDE_AT_EQUATOR = 111319.488;
    private static final double CELL_SIZE_METERS = 1000.0; // 1km

    private static final Logger LOGGER = LoggerFactory.getLogger(GridUtil.class);


    public GridCellInfo getGridCellForCoordinates(double latitude, double longitude) {

        // 1. Calculate approximate 1km spacing in degrees at this latitude
        double latitudeRadians = Math.toRadians(latitude);
        double metersPerDegreeLongitude = METERS_PER_DEGREE_LONGITUDE_AT_EQUATOR * Math.cos(latitudeRadians);

        // Avoid division by zero near poles (not relevant for Austria, but good practice)
        if (metersPerDegreeLongitude < 1.0) {
            metersPerDegreeLongitude = 1.0;
        }

        double latSpacingDegrees = CELL_SIZE_METERS / METERS_PER_DEGREE_LATITUDE;
        double lonSpacingDegrees = CELL_SIZE_METERS / metersPerDegreeLongitude;

        // 2. Snap input coordinates to the nearest grid center
        // We assume grid centers are aligned at intervals of spacingDegrees from origin 0,0
        // (Or adjust using REFERENCE_LATITUDE/LONGITUDE if defined)
        double snappedCenterLat = Math.round(latitude / latSpacingDegrees) * latSpacingDegrees;
        double snappedCenterLon = Math.round(longitude / lonSpacingDegrees) * lonSpacingDegrees;

        // Use snapped center as the target coordinates
        double targetLatitude = snappedCenterLat;
        double targetLongitude = snappedCenterLon;

        // 3. Generate Cell ID based on the snapped center (use sufficient precision)
        String cellId = String.format(Locale.US, "cell_%.6f_%.6f", targetLatitude, targetLongitude);

        // 4. Calculate Bbox corners based on snapped center and spacing
        // Use half the spacing calculated *at the snapped center latitude* for longitude
        double centerLatRadians = Math.toRadians(targetLatitude);
        double metersPerDegreeLonAtCenter = METERS_PER_DEGREE_LONGITUDE_AT_EQUATOR * Math.cos(centerLatRadians);
        if (metersPerDegreeLonAtCenter < 1.0) metersPerDegreeLonAtCenter = 1.0;

        double halfLatSpacing = (CELL_SIZE_METERS / 2.0) / METERS_PER_DEGREE_LATITUDE;
        double halfLonSpacing = (CELL_SIZE_METERS / 2.0) / metersPerDegreeLonAtCenter;

        // Add a small buffer (e.g., 10%) to ensure the API captures the center point
        // This helps account for potential minor discrepancies between our calculation
        // and the API's internal grid definition or coordinate precision.
        double bufferFactor = 1.1;
        double bufferedHalfLat = halfLatSpacing * bufferFactor;
        double bufferedHalfLon = halfLonSpacing * bufferFactor;

        double minLat = targetLatitude - bufferedHalfLat;
        double maxLat = targetLatitude + bufferedHalfLat;
        double minLon = targetLongitude - bufferedHalfLon;
        double maxLon = targetLongitude + bufferedHalfLon;

        BoundingBox bbox = new BoundingBox(minLat, minLon, maxLat, maxLon);

        // Log the calculated values for debugging
         System.out.printf("Input: (%.6f, %.6f)\n", latitude, longitude);
         System.out.printf("Snapped Center: (%.6f, %.6f)\n", targetLatitude, targetLongitude);
         System.out.printf("BBox: %s\n", bbox.toApiString());
         System.out.printf("CellID: %s\n", cellId);

        return new GridCellInfo(cellId, bbox, targetLatitude, targetLongitude);
    }

    public List<GridCellInfo> generateGrid(BoundingBox boundingBox, double gridResolution) {

        List<GridCellInfo> gridCells = new ArrayList<>();

        LOGGER.info("Generating grid for BoundingBox: {} with resolution: {}",
                boundingBox.toApiString(), gridResolution);

        double minLat = boundingBox.getMinLat();
        double maxLat = boundingBox.getMaxLat();
        double minLon = boundingBox.getMinLon();
        double maxLon = boundingBox.getMaxLon();
        Set<String> seen = new HashSet<>();

        for (double lat = minLat; lat <= maxLat; lat += gridResolution) {
            for (double lon = minLon; lon <= maxLon; lon += gridResolution) {
                GridCellInfo cell = getGridCellForCoordinates(lat, lon);
                if (seen.add(cell.getCellId())) {
                    gridCells.add(cell);
                }
            }
        }

        LOGGER.info("Generated {} grid cells", gridCells.size());
        return gridCells;

    }

}
