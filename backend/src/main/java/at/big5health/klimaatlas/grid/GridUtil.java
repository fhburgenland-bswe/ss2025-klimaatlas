package at.big5health.klimaatlas.grid;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.*;

/**
 * Utility class providing methods for geographical grid calculations.
 * <p>
 * This component offers functionalities such as determining a grid cell
 * for specific coordinates and generating a list of grid cells within a
 * given bounding box. It uses approximate calculations for grid spacing
 * based on a defined cell size.
 *
 * @see GridCellInfo
 * @see BoundingBox
 */
@Component
public class GridUtil {

    /**
     * Approximate meters per degree of latitude. This value is relatively constant.
     */
    private static final double METERS_PER_DEGREE_LATITUDE = 111132.954;
    /**
     * Approximate meters per degree of longitude at the Earth's equator.
     * This value varies with latitude.
     */
    private static final double METERS_PER_DEGREE_LONGITUDE_AT_EQUATOR = 111319.488;
    /**
     * The target size for each grid cell, in meters (e.g., 1000.0 for 1km x 1km cells).
     */
    private static final double CELL_SIZE_METERS = 1000.0;

    private static final Logger LOGGER = LoggerFactory.getLogger(GridUtil.class);

    /**
     * Calculates the grid cell information for a given pair of latitude and longitude coordinates.
     * <p>
     * The method performs the following steps:
     * <ol>
     *   <li>Calculates approximate 1km grid spacing in degrees at the given latitude.</li>
     *   <li>Snaps the input coordinates to the center of the nearest conceptual grid cell.
     *       These snapped coordinates become the target latitude/longitude for the cell.</li>
     *   <li>Generates a unique cell ID based on these target coordinates.</li>
     *   <li>Calculates a bounding box around the target coordinates, representing the cell's extent.
     *       A small buffer (10%) is added to this bounding box to help ensure that API calls
     *       using this box reliably capture data for the target point, accounting for potential
     *       minor discrepancies in grid definitions or coordinate precision between systems.</li>
     * </ol>
     * Note: The grid is conceptualized as cells of {@value #CELL_SIZE_METERS} meters.
     *
     * @param latitude  The input geographical latitude, in decimal degrees.
     * @param longitude The input geographical longitude, in decimal degrees.
     * @return A {@link GridCellInfo} object containing the cell ID, its bounding box,
     *         and the target (snapped center) coordinates.
     */
    public GridCellInfo getGridCellForCoordinates(double latitude, double longitude) {
        // 1. Calculate approximate 1km spacing in degrees at this latitude
        double latitudeRadians = Math.toRadians(latitude);
        double metersPerDegreeLongitude = METERS_PER_DEGREE_LONGITUDE_AT_EQUATOR * Math.cos(latitudeRadians);

        if (metersPerDegreeLongitude < 1.0) { // Avoid division by zero near poles
            metersPerDegreeLongitude = 1.0;
        }

        double latSpacingDegrees = CELL_SIZE_METERS / METERS_PER_DEGREE_LATITUDE;
        double lonSpacingDegrees = CELL_SIZE_METERS / metersPerDegreeLongitude;

        // 2. Snap input coordinates to the nearest grid center
        double snappedCenterLat = Math.round(latitude / latSpacingDegrees) * latSpacingDegrees;
        double snappedCenterLon = Math.round(longitude / lonSpacingDegrees) * lonSpacingDegrees;

        double targetLatitude = snappedCenterLat;
        double targetLongitude = snappedCenterLon;

        // 3. Generate Cell ID
        String cellId = String.format(Locale.US, "cell_%.6f_%.6f", targetLatitude, targetLongitude);

        // 4. Calculate Bbox corners
        double centerLatRadians = Math.toRadians(targetLatitude);
        double metersPerDegreeLonAtCenter = METERS_PER_DEGREE_LONGITUDE_AT_EQUATOR * Math.cos(centerLatRadians);
        if (metersPerDegreeLonAtCenter < 1.0) metersPerDegreeLonAtCenter = 1.0;

        double halfLatSpacing = (CELL_SIZE_METERS / 2.0) / METERS_PER_DEGREE_LATITUDE;
        double halfLonSpacing = (CELL_SIZE_METERS / 2.0) / metersPerDegreeLonAtCenter;

        double bufferFactor = 1.1; // 10% buffer
        double bufferedHalfLat = halfLatSpacing * bufferFactor;
        double bufferedHalfLon = halfLonSpacing * bufferFactor;

        double minLat = targetLatitude - bufferedHalfLat;
        double maxLat = targetLatitude + bufferedHalfLat;
        double minLon = targetLongitude - bufferedHalfLon;
        double maxLon = targetLongitude + bufferedHalfLon;

        BoundingBox bbox = new BoundingBox(minLat, minLon, maxLat, maxLon);

        // Logging for debugging (consider removing or reducing for production)
        // System.out.printf("Input: (%.6f, %.6f)\n", latitude, longitude);
        // System.out.printf("Snapped Center: (%.6f, %.6f)\n", targetLatitude, targetLongitude);
        // System.out.printf("BBox: %s\n", bbox.toApiString());
        // System.out.printf("CellID: %s\n", cellId);

        return new GridCellInfo(cellId, bbox, targetLatitude, targetLongitude);
    }

    /**
     * Generates a list of unique grid cells that cover a given geographical bounding box,
     * based on a specified grid resolution.
     * <p>
     * This method iterates over the bounding box using the provided {@code gridResolution}
     * (in degrees) as a step. For each point in this iteration, it determines the
     * corresponding grid cell using {@link #getGridCellForCoordinates(double, double)}.
     * Duplicate cells (based on cell ID) are avoided to ensure each unique grid cell
     * that intersects or is near the iteration points is included only once.
     *
     * @param boundingBox    The {@link BoundingBox} defining the overall area for which
     *                       to generate grid cells.
     * @param gridResolution The step size in decimal degrees for iterating across
     *                       the latitude and longitude of the bounding box. A smaller
     *                       value will result in more iteration points and potentially
     *                       more fine-grained coverage.
     * @return A list of unique {@link GridCellInfo} objects representing the grid cells.
     *         The list may be empty if the bounding box is invalid or the resolution
     *         results in no cells.
     */
    public List<GridCellInfo> generateGrid(BoundingBox boundingBox, double gridResolution) {
        List<GridCellInfo> gridCells = new ArrayList<>();
        LOGGER.info("Generating grid for BoundingBox: {} with resolution: {}",
                boundingBox.toApiString(), gridResolution);

        double minLat = boundingBox.getMinLat();
        double maxLat = boundingBox.getMaxLat();
        double minLon = boundingBox.getMinLon();
        double maxLon = boundingBox.getMaxLon();
        Set<String> seenCellIds = new HashSet<>(); // To track unique cell IDs

        for (double lat = minLat; lat <= maxLat; lat += gridResolution) {
            for (double lon = minLon; lon <= maxLon; lon += gridResolution) {
                GridCellInfo cell = getGridCellForCoordinates(lat, lon);
                if (seenCellIds.add(cell.getCellId())) { // Add to list only if cellId is new
                    gridCells.add(cell);
                }
            }
        }

        LOGGER.info("Generated {} unique grid cells for BoundingBox: {}",
                gridCells.size(), boundingBox.toApiString());
        return gridCells;
    }
}
