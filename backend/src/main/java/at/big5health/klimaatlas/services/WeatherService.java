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

@Service
public class WeatherService {

    private static final Logger LOG = LoggerFactory.getLogger(WeatherService.class);

    private final ExternalWeatherApiClient externalClient;
    private final GridUtil gridUtil;

    public WeatherService(ExternalWeatherApiClient externalClient, GridUtil gridUtil) {
        this.externalClient = externalClient;
        this.gridUtil = gridUtil;
    }

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

    public WeatherReportDTO getWeatherReportDTO(double latitude, double longitude) {
        // Verwende die Hauptmethode mit dem aktuellen Datum
        return getWeather(null, longitude, latitude, LocalDate.now());
    }

    // Helper method to find the closest feature
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

    // Simple distance squared calculation (good enough for local comparisons)
    private double distanceSquared(double lat1, double lon1, double lat2, double lon2) {
        double latDiff = lat1 - lat2;
        double lonDiff = lon1 - lon2;
        // Basic Pythagorean theorem - ignores Earth's curvature, but okay for 1km scale comparison
        return (latDiff * latDiff) + (lonDiff * lonDiff);
    }

    // extractWeatherDataFromFeature and mapPrecipitation remain the same
    private WeatherReportDTO extractWeatherDataFromFeature(SpartacusFeature feature) {
        Map<String, SpartacusParameter> params = feature.getProperties().getParameters();
        Double minTemp = params.containsKey("TN") ? params.get("TN").getData().get(0) : null;
        Double maxTemp = params.containsKey("TX") ? params.get("TX").getData().get(0) : null;
        Double precipValue = params.containsKey("RR") ? params.get("RR").getData().get(0) : null;
        Double sunDuration = null; // Placeholder
        Precipitation precipEnum = mapPrecipitation(precipValue);
        return new WeatherReportDTO(minTemp, maxTemp, precipEnum, sunDuration, null, null);
    }

    private Precipitation mapPrecipitation(Double precipValue) {
        if (precipValue == null || precipValue <= 0.0) return Precipitation.NONE;
        if (precipValue > 5.0) return Precipitation.RAIN;
        if (precipValue > 0.0) return Precipitation.DRIZZLE;
        return Precipitation.NONE;
    }

}
