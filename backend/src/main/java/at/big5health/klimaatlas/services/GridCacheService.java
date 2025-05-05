package at.big5health.klimaatlas.services;

import at.big5health.klimaatlas.dtos.WeatherReportDTO;
import at.big5health.klimaatlas.grid.BoundingBox;
import at.big5health.klimaatlas.grid.GridCellInfo;
import at.big5health.klimaatlas.grid.GridUtil;
import at.big5health.klimaatlas.models.WeatherReport;
import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@Service
public class GridCacheService {

    private static final Logger logger = LoggerFactory.getLogger(GridCacheService.class);

    @Autowired
    private WeatherService weatherService;

    @Autowired
    private GridUtil gridUtil;

    @Autowired
    private CacheManager cacheManager;

    // Definition der österreichischen Bundesländer mit deren Bounding Boxes
    private final Map<String, BoundingBox> austrianStates = new HashMap<String, BoundingBox>() {{
        put("Niederösterreich", new BoundingBox(47.4, 48.7, 14.4, 17.2));
        put("Wien", new BoundingBox(48.1, 48.3, 16.2, 16.6));
        put("Burgenland", new BoundingBox(46.7, 48.1, 16.0, 17.2));
        put("Steiermark", new BoundingBox(46.6, 47.9, 13.6, 16.2));
        put("Oberösterreich", new BoundingBox(47.4, 48.8, 12.7, 14.9));
        put("Salzburg", new BoundingBox(46.8, 47.9, 12.5, 13.8));
        put("Kärnten", new BoundingBox(46.4, 47.2, 12.6, 15.0));
        put("Tirol", new BoundingBox(46.6, 47.8, 10.0, 13.0));
        put("Vorarlberg", new BoundingBox(46.8, 47.6, 9.5, 10.3));
    }};

    private final ExecutorService executorService = Executors.newFixedThreadPool(4);

    // Diese Methode wird beim Start der Anwendung ausgeführt
    @PostConstruct
    public void initializeGridCache() {
        logger.info("Initializing temperature grid cache for all Austrian states");

        // Parallel laden der Daten für alle Bundesländer
        List<CompletableFuture<Void>> futures = new ArrayList<>();

        for (Map.Entry<String, BoundingBox> state : austrianStates.entrySet()) {
            CompletableFuture<Void> future = CompletableFuture.runAsync(() -> {
                try {
                    logger.info("Loading temperature grid for state: {}", state.getKey());
                    getTemperatureGridForState(state.getKey());
                    logger.info("Successfully loaded temperature grid for state: {}", state.getKey());
                } catch (Exception e) {
                    logger.error("Failed to load temperature grid for state: {}", state.getKey(), e);
                }
            }, executorService);

            futures.add(future);
        }

        // Warten bis alle Daten geladen sind (optional)
        CompletableFuture.allOf(futures.toArray(new CompletableFuture[0])).join();
        logger.info("Temperature grid cache initialization completed");
    }

    // Diese Methode wird jeden Tag um 10:00 Uhr ausgeführt
    @Scheduled(cron = "0 0 10 * * ?")
    @CacheEvict(value = "temperatureGrid", allEntries = true)
    public void refreshCache() {
        logger.info("Refreshing temperature grid cache");
        initializeGridCache();
    }

    @Cacheable(value = "temperatureGrid", key = "#stateName")
    public List<GridTemperature> getTemperatureGridForState(String stateName) {
        BoundingBox boundingBox = austrianStates.get(stateName);
        if (boundingBox == null) {
            throw new IllegalArgumentException("Unknown state: " + stateName);
        }

        // Berechne ein Raster von Punkten innerhalb der Bounding Box
        // mit einer angemessenen Auflösung (z.B. alle 0.1 Grad)
        double gridResolution = 0.1; // ca. 10km
        List<GridCellInfo> gridCells = gridUtil.generateGrid(boundingBox, gridResolution);

        List<GridTemperature> gridTemperatures = new ArrayList<>();
        LocalDate today = LocalDate.now();

        for (GridCellInfo cell : gridCells) {
            try {
                // Verwende die getWeather-Methode statt getWeatherReport
                double lat = cell.getTargetLatitude();
                double lon = cell.getTargetLongitude();

                // Angepasst an die vorhandene WeatherService API
                WeatherReportDTO report = weatherService.getWeather(null, lon, lat, today);

                // Extrahiere die Temperatur aus dem WeatherReportDTO
                // Verwende den Mittelwert aus Min und Max, wenn verfügbar
                Double temperature = null;
                if (report.getMinTemp() != null && report.getMaxTemp() != null) {
                    temperature = (report.getMinTemp() + report.getMaxTemp()) / 2.0;
                } else if (report.getMaxTemp() != null) {
                    temperature = report.getMaxTemp();
                } else if (report.getMinTemp() != null) {
                    temperature = report.getMinTemp();
                }

                if (temperature != null) {
                    gridTemperatures.add(new GridTemperature(
                            cell.getTargetLatitude(),
                            cell.getTargetLongitude(),
                            temperature));
                }
            } catch (Exception e) {
                // Fehler protokollieren, aber weitermachen
                logger.warn("Could not fetch temperature data for point {}, {}",
                        cell.getTargetLatitude(), cell.getTargetLongitude(), e);
            }
        }

        return gridTemperatures;
    }

    public List<GridTemperature> getAllTemperatureGridPoints() {
        List<GridTemperature> allPoints = new ArrayList<>();

        for (String state : austrianStates.keySet()) {
            try {
                List<GridTemperature> statePoints = getTemperatureGridForState(state);
                allPoints.addAll(statePoints);
            } catch (Exception e) {
                logger.error("Error retrieving temperature grid for state: {}", state, e);
            }
        }

        return allPoints;
    }

    // Innere Klasse zur Repräsentation eines Temperaturpunktes im Raster
    public static class GridTemperature {
        private final double latitude;
        private final double longitude;
        private final double temperature;

        public GridTemperature(double latitude, double longitude, double temperature) {
            this.latitude = latitude;
            this.longitude = longitude;
            this.temperature = temperature;
        }

        public double getLatitude() {
            return latitude;
        }

        public double getLongitude() {
            return longitude;
        }

        public double getTemperature() {
            return temperature;
        }
    }

}
