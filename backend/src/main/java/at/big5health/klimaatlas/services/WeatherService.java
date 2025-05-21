package at.big5health.klimaatlas.services;

import at.big5health.klimaatlas.httpclients.ExternalWeatherApiClient;
import at.big5health.klimaatlas.dtos.Precipitation;
import at.big5health.klimaatlas.dtos.WeatherReportDTO;
import at.big5health.klimaatlas.dtos.spartacus.SpartacusFeature;
import at.big5health.klimaatlas.dtos.spartacus.SpartacusFeatureCollection;
import at.big5health.klimaatlas.dtos.spartacus.SpartacusParameter;
import at.big5health.klimaatlas.exceptions.ErrorMessages;
import at.big5health.klimaatlas.exceptions.ExternalApiException;
import at.big5health.klimaatlas.exceptions.WeatherDataNotFoundException;
import at.big5health.klimaatlas.grid.BoundingBox;
import at.big5health.klimaatlas.grid.GridCellInfo;
import at.big5health.klimaatlas.grid.GridUtil;
import at.big5health.klimaatlas.models.WeatherReport;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Service layer responsible for fetching, processing, and caching weather data.
 * <p>
 * This service orchestrates interactions with the {@link ExternalWeatherApiClient}
 * to retrieve raw weather data and uses {@link GridUtil} to map geographical
 * coordinates to specific grid cells. It employs caching mechanisms
 * (e.g., "weatherCache", "dailyWeatherDataGrid") to optimize performance and
 * reduce external API calls. The service primarily returns weather data
 * as {@link WeatherReportDTO} objects.
 *
 * @see ExternalWeatherApiClient
 * @see GridUtil
 * @see WeatherReportDTO
 * @see WeatherReport
 * @see Precipitation
 * @since 1.0.0
 */
@Service
public class WeatherService {

    private static final Logger LOG = LoggerFactory.getLogger(WeatherService.class);

    private final ExternalWeatherApiClient externalClient;
    private final GridUtil gridUtil;

    /**
     * Constructs a {@code WeatherService} with the necessary dependencies.
     *
     * @param externalClient The client for fetching data from the external weather API.
     * @param gridUtil       The utility for grid-based calculations.
     */
    public WeatherService(ExternalWeatherApiClient externalClient, GridUtil gridUtil) {
        this.externalClient = externalClient;
        this.gridUtil = gridUtil;
    }

    /**
     * Retrieves a weather report for the given coordinates and date.
     * <p>
     * This method first maps the provided latitude and longitude to a specific
     * {@link GridCellInfo} using {@link GridUtil}. It then attempts to fetch
     * or retrieve from cache the weather data for this grid cell's target coordinates
     * via {@link #getOrFetchGridCellData(String, BoundingBox, LocalDate, double, double)}.
     * The final {@link WeatherReportDTO} returned will have its latitude and longitude
     * fields set to the original input coordinates, while other weather data
     * pertains to the determined grid cell.
     * <p>
     * Results are cached under "weatherCache" based on the input latitude, longitude, and date.
     *
     * @param cityName   The name of the city (currently unused in core logic but logged).
     *                   Can be {@code null}.
     * @param longitude  The geographical longitude for the weather report, in decimal degrees.
     * @param latitude   The geographical latitude for the weather report, in decimal degrees.
     * @param actualDate The specific date for which the weather report is requested.
     * @return A {@link WeatherReportDTO} containing the weather data.
     * @throws WeatherDataNotFoundException if no weather data can be found for the
     *                                      determined grid cell and date.
     * @throws ExternalApiException         if an error occurs during grid calculation or
     *                                      while interacting with the external API.
     * @see GridUtil#getGridCellForCoordinates(double, double)
     * @see #getOrFetchGridCellData(String, BoundingBox, LocalDate, double, double)
     * @see Cacheable
     */
    @Cacheable(value = "weatherCache", key = "#latitude + '_' + #longitude + '_' + #actualDate", unless = "#result == null")
    public WeatherReportDTO getWeather(String cityName, Double longitude, Double latitude, LocalDate actualDate) {
        LOG.info("Request received for city: {}, lat: {}, lon: {}, date: {}", cityName, latitude, longitude, actualDate);

        GridCellInfo gridCell;
        try {
            // Pass the original lat/lon to GridUtil
            gridCell = gridUtil.getGridCellForCoordinates(latitude, longitude);
            LOG.debug("Mapped to grid cell: {} with target center ({}, {})",
                    gridCell.getCellId(), gridCell.getTargetLatitude(), gridCell.getTargetLongitude());
        } catch (Exception e) {
            LOG.error("Error calculating grid cell for lat={}, lon={}: {}", latitude, longitude, e.getMessage(), e);
            throw new ExternalApiException(ErrorMessages.GRID_UTIL_ERROR, e);
        }

        // Pass the target coordinates from GridCellInfo to the fetching method
        Optional<WeatherReportDTO> cellDataOpt = getOrFetchGridCellData(
                gridCell.getCellId(),
                gridCell.getBbox(),
                actualDate,
                gridCell.getTargetLatitude(), // Pass target lat
                gridCell.getTargetLongitude() // Pass target lon
        );

        if (cellDataOpt.isPresent()) {
            WeatherReportDTO cellData = cellDataOpt.get();
            return new WeatherReportDTO(
                    cellData.getMinTemp(), cellData.getMaxTemp(), cellData.getPrecip(),
                    cellData.getSunDuration(), latitude, longitude // Use original request lat/lon for final response
            );
        } else {
            LOG.warn("No weather data found for grid cell {} on date {}", gridCell.getCellId(), actualDate);
            throw new WeatherDataNotFoundException(ErrorMessages.WEATHER_DATA_NOT_FOUND);
        }
    }

    /**
     * Retrieves a {@link WeatherReport} model object for the given coordinates for today's date.
     * <p>
     * This method calls {@link #getWeather(String, Double, Double, LocalDate)} to get
     * the {@link WeatherReportDTO} and then maps it to a {@link WeatherReport} model.
     *
     * @param latitude  The geographical latitude, in decimal degrees.
     * @param longitude The geographical longitude, in decimal degrees.
     * @return A {@link WeatherReport} object.
     * @throws ExternalApiException if an error occurs during data fetching or processing.
     * @deprecated Consider directly using methods returning {@link WeatherReportDTO}
     *             if the {@code WeatherReport} model is not strictly needed,
     *             to reduce object mapping.
     */
    @Deprecated
    public WeatherReport getWeatherReport(double latitude, double longitude) {
        try {

            LocalDate today = LocalDate.now();

            WeatherReportDTO dto = getWeather(null, longitude, latitude, today);

            WeatherReport report = new WeatherReport();

            report.setMaxTemp(dto.getMaxTemp());
            report.setMinTemp(dto.getMinTemp());
            report.setLatitude(dto.getLatitude());
            report.setLongitude(dto.getLongitude());
            report.setPrecip(dto.getPrecip());
            report.setSunDuration(dto.getSunDuration());

            return report;
        } catch (Exception e) {
            LOG.error("Error getting weather report for lat={}, lon={}: {}", latitude, longitude, e.getMessage());
            throw new ExternalApiException("Error fetching weather data for coordinates", e);
        }
    }

    /**
     * Retrieves or fetches weather data for a specific grid cell identified by its ID,
     * bounding box, date, and target coordinates.
     * <p>
     * This method attempts to retrieve data from the "dailyWeatherDataGrid" cache.
     * If a cache miss occurs ({@code sync = true} ensures only one thread fetches),
     * it calls the {@link ExternalWeatherApiClient#fetchGridData(BoundingBox, LocalDate)}
     * method. From the returned {@link SpartacusFeatureCollection}, it finds the feature
     * closest to the {@code targetLat} and {@code targetLon} and extracts weather data
     * using {@link #extractWeatherDataFromFeature(SpartacusFeature)}.
     *
     * @param cellId     The unique ID of the grid cell. Used as part of the cache key.
     * @param bbox       The {@link BoundingBox} of the grid cell, passed to the external client.
     * @param actualDate The date for which data is requested.
     * @param targetLat  The target latitude within the cell, used for finding the closest feature.
     * @param targetLon  The target longitude within the cell, used for finding the closest feature.
     * @return An {@link Optional} containing the {@link WeatherReportDTO} if data is found
     *         and processed successfully, or an empty {@link Optional} otherwise.
     * @throws ExternalApiException if an error occurs during interaction with the external API
     *                              or during data processing.
     * @see Cacheable
     * @see ExternalWeatherApiClient#fetchGridData(BoundingBox, LocalDate)
     * @see #findClosestFeature(List, double, double)
     * @see #extractWeatherDataFromFeature(SpartacusFeature)
     */
    @Cacheable(value = "dailyWeatherDataGrid", key = "#cellId + '_' + #actualDate", sync = true)
    public Optional<WeatherReportDTO> getOrFetchGridCellData(
            String cellId, BoundingBox bbox, LocalDate actualDate, double targetLat, double targetLon) {

        LOG.info("CACHE MISS for grid: {}, Date: {}. Calling external API.", cellId, actualDate);

        try {
            SpartacusFeatureCollection featureCollection = externalClient.fetchGridData(bbox, actualDate).block();

            if (featureCollection == null || featureCollection.getFeatures() == null || featureCollection.getFeatures().isEmpty()) {
                LOG.warn("External API returned no features for grid: {}, Date: {}", cellId, actualDate);
                return Optional.empty(); // No data found for this cell/date
            }

            // --- Find the feature closest to the target coordinates ---
            Optional<SpartacusFeature> closestFeatureOpt = findClosestFeature(
                    featureCollection.getFeatures(), targetLat, targetLon
            );

            if (closestFeatureOpt.isEmpty()) {
                LOG.warn("Could not find a suitable feature within the response for grid: {}, Date: {}", cellId, actualDate);
                return Optional.empty(); // No suitable feature found
            }
            // --- ---

            SpartacusFeature feature = closestFeatureOpt.get();
            WeatherReportDTO report = extractWeatherDataFromFeature(feature);
            LOG.debug("Successfully fetched and processed data for grid: {}, Date: {}", cellId, actualDate);
            return Optional.of(report);

        } catch (ExternalApiException e) {
            LOG.error("External API Exception during fetch for grid {}: {}", cellId, e.getMessage(), e);
            throw e;
        } catch (Exception e) {
            LOG.error("Unexpected error fetching/processing data for grid {}: {}", cellId, e.getMessage(), e);
            throw new ExternalApiException(ErrorMessages.UNEXPECTED_ERROR, e);
        }
    }

    /**
     * Retrieves a {@link WeatherReportDTO} for the given coordinates for the current date.
     * <p>
     * This is a convenience method that calls the primary
     * {@link #getWeather(String, Double, Double, LocalDate)} method with today's date.
     *
     * @param latitude  The geographical latitude, in decimal degrees.
     * @param longitude The geographical longitude, in decimal degrees.
     * @return A {@link WeatherReportDTO} for the current date and specified coordinates.
     */
    public WeatherReportDTO getWeatherReportDTO(double latitude, double longitude) {
        // Verwende die Hauptmethode mit dem aktuellen Datum
        return getWeather(null, longitude, latitude, LocalDate.now());
    }

    /**
     * Finds the {@link SpartacusFeature} from a list that is geographically closest
     * to the given target latitude and longitude.
     * <p>
     * Closeness is determined by the minimum squared Euclidean distance to avoid
     * computationally more expensive square root operations. This is suitable for
     * comparing relative distances over small areas.
     *
     * @param features  A list of {@link SpartacusFeature} objects to search within.
     * @param targetLat The target latitude.
     * @param targetLon The target longitude.
     * @return An {@link Optional} containing the closest {@link SpartacusFeature},
     *         or an empty {@link Optional} if the input list is null or empty.
     */
    private Optional<SpartacusFeature> findClosestFeature(List<SpartacusFeature> features, double targetLat, double targetLon) {
        if (features == null || features.isEmpty()) {
            return Optional.empty();
        }

        // Use stream to find the feature with the minimum distance squared
        // (avoids square root calculation for comparison)
        return features.stream()
                .min(Comparator.comparingDouble(feature ->
                        distanceSquared(
                                feature.getGeometry().getCoordinates().get(1), // latitude is index 1
                                feature.getGeometry().getCoordinates().get(0), // longitude is index 0
                                targetLat,
                                targetLon
                        )
                ));
    }

    /**
     * Calculates the squared Euclidean distance between two geographical points.
     * <p>
     * This is a simplified distance calculation that does not account for Earth's
     * curvature but is generally sufficient for comparing relative distances
     * between points that are close to each other (e.g., within a few kilometers).
     * Using squared distance avoids a square root operation, making comparisons faster.
     *
     * @param lat1 Latitude of the first point.
     * @param lon1 Longitude of the first point.
     * @param lat2 Latitude of the second point.
     * @param lon2 Longitude of the second point.
     * @return The squared distance between the two points.
     */
    private double distanceSquared(double lat1, double lon1, double lat2, double lon2) {
        double latDiff = lat1 - lat2;
        double lonDiff = lon1 - lon2;
        // Basic Pythagorean theorem - ignores Earth's curvature, but okay for 1km scale comparison
        return (latDiff * latDiff) + (lonDiff * lonDiff);
    }

    /**
     * Helper method to safely extract the first data value from a SpartacusParameter.
     *
     * @param params        The map of parameters.
     * @param parameterName The name of the parameter to extract (e.g., "TN", "TX", "SA").
     * @return The Double value of the parameter, or null if not found or data is missing.
     */
    private Double getParameterValue(Map<String, SpartacusParameter> params, String parameterName) {
        if (params.containsKey(parameterName)) {
            SpartacusParameter param = params.get(parameterName);
            if (param != null && param.getData() != null && !param.getData().isEmpty()) {
                // Assuming the first value in the list is the one we need.
                return param.getData().get(0);
            } else {
                LOG.warn("Parameter '{}' is present but has no data or data list is empty.", parameterName);
            }
        }
        return null; // Parameter not found or no data
    }


    /**
     * Extracts weather data from a {@link SpartacusFeature} and maps it to a
     * {@link WeatherReportDTO}.
     * <p>
     * It retrieves parameters like minimum temperature ("TN"), maximum temperature ("TX"),
     * precipitation ("RR"), and sun duration ("SA") from the feature's properties.
     * Latitude and longitude in the returned DTO are set to {@code null} as they are
     * expected to be set by the calling method based on the original request or cell context.
     *
     * @param feature The {@link SpartacusFeature} from which to extract data.
     * @return A {@link WeatherReportDTO} populated with data from the feature.
     * @see #mapPrecipitation(Double)
     */
    private WeatherReportDTO extractWeatherDataFromFeature(SpartacusFeature feature) {
        Map<String, SpartacusParameter> params = feature.getProperties().getParameters();

        Double minTemp = getParameterValue(params, "TN");
        Double maxTemp = getParameterValue(params, "TX");
        Double precipValue = getParameterValue(params, "RR");
        Double sunDurationSeconds = getParameterValue(params, "SA"); // Extract "SA" for sun duration

        Precipitation precipEnum = mapPrecipitation(precipValue); // Keep existing enum mapping

        // Latitude and longitude are null here; they will be set by the calling method
        // (getWeather) to the original request's coordinates.
        return new WeatherReportDTO(minTemp, maxTemp, precipEnum, sunDurationSeconds, null, null);
    }

    /**
     * Maps a raw precipitation value (typically in mm) to a {@link Precipitation} enum.
     * <p>
     * Current mapping logic:
     * <ul>
     *   <li>{@code null} or <= 0.0: {@link Precipitation#NONE}</li>
     *   <li>> 0.0 and <= 5.0: {@link Precipitation#DRIZZLE}</li>
     *   <li>> 5.0: {@link Precipitation#RAIN}</li>
     * </ul>
     * This logic may need adjustment based on specific API definitions or requirements.
     *
     * @param precipValue The raw precipitation value, usually in millimeters.
     *                    Can be {@code null}.
     * @return The corresponding {@link Precipitation} enum value.
     */
    private Precipitation mapPrecipitation(Double precipValue) {
        if (precipValue == null || precipValue <= 0.0) return Precipitation.NONE;
        if (precipValue > 5.0) return Precipitation.RAIN;
        if (precipValue > 0.0) return Precipitation.DRIZZLE;
        return Precipitation.NONE;
    }
}
