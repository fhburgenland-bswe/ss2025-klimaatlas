package at.big5health.klimaatlas.services;

import at.big5health.klimaatlas.dtos.WeatherReportDTO;
import at.big5health.klimaatlas.grid.BoundingBox;
import at.big5health.klimaatlas.grid.GridCellInfo;
import at.big5health.klimaatlas.grid.GridUtil;
import jakarta.annotation.PreDestroy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.context.annotation.Lazy;
import org.springframework.context.event.EventListener;
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

    private static final Logger LOGGER = LoggerFactory.getLogger(GridCacheService.class);

    public record GridTemperature(double latitude, double longitude, double temperature) {}

    @Autowired
    protected WeatherService weatherService;

    @Autowired
    protected GridUtil gridUtil;

    @Autowired
    private CacheManager cacheManager;

    @Autowired
    @Lazy
    private GridCacheService selfProxy;

    private final Map<String, BoundingBox> austrianStates = new HashMap<String, BoundingBox>() {{
        put("Niederösterreich", new BoundingBox(47.4, 14.4, 48.7, 17.2));
        put("Wien", new BoundingBox(48.1, 16.2, 48.3, 16.6));
        put("Burgenland", new BoundingBox(46.7, 16.0, 48.1,17.2));
        put("Steiermark", new BoundingBox(46.6, 13.6, 47.9, 16.2));
        put("Oberösterreich", new BoundingBox(47.4, 12.7, 48.8, 14.9));
        put("Salzburg", new BoundingBox(46.8, 12.5, 47.9,13.8));
        put("Kärnten", new BoundingBox(46.4, 12.6, 47.2, 15.0));
        put("Tirol", new BoundingBox(46.6, 10.0, 47.8, 13.0));
        put("Vorarlberg", new BoundingBox(46.8, 9.5, 47.6,10.3));
    }};

    private final ExecutorService executorService = Executors.newFixedThreadPool(4);

    @EventListener(ApplicationReadyEvent.class)
    public void initializeGridCache() {
        LOGGER.info("Initializing temperature grid cache for all Austrian states");

        List<CompletableFuture<Void>> futures = new ArrayList<>();

        for (Map.Entry<String, BoundingBox> state : austrianStates.entrySet()) {
            CompletableFuture<Void> future = CompletableFuture.runAsync(() -> {
                try {
                    LOGGER.info("Loading temperature grid for state: {}", state.getKey());
                    selfProxy.getTemperatureGridForState(state.getKey());
                    LOGGER.info("Successfully loaded temperature grid for state: {}", state.getKey());
                } catch (Exception e) {
                    LOGGER.error("Failed to load temperature grid for state: {}", state.getKey(), e);
                }
            }, executorService);

            futures.add(future);
        }

        CompletableFuture.allOf(futures.toArray(new CompletableFuture[0])).join();
        LOGGER.info("Temperature grid cache initialization completed");
    }

    @Scheduled(cron = "0 0 10 * * ?")
    @CacheEvict(value = "temperatureGrid", allEntries = true)
    public void refreshCache() {
        LOGGER.info("Refreshing temperature grid cache");
        initializeGridCache();
    }

    @Cacheable(value = "temperatureGrid", key = "#stateName")
    public List<GridTemperature> getTemperatureGridForState(String stateName) {
        BoundingBox boundingBox = austrianStates.get(stateName);
        if (boundingBox == null) {
            throw new IllegalArgumentException("Unknown state: " + stateName);
        }

        double gridResolution = 1.0; // 0.1 = too many requests
        List<GridCellInfo> gridCells = gridUtil.generateGrid(boundingBox, gridResolution);

        if (gridCells.isEmpty()) {
            LOGGER.warn("No grid cells generated for state: {}. Using fallback approach.", stateName);
            double centerLat = (boundingBox.getMinLat() + boundingBox.getMaxLat()) / 2.0;
            double centerLon = (boundingBox.getMinLon() + boundingBox.getMaxLon()) / 2.0;
            GridCellInfo centerCell = gridUtil.getGridCellForCoordinates(centerLat, centerLon);
            gridCells.add(centerCell);
        }

        List<GridTemperature> gridTemperatures = new ArrayList<>();
        LocalDate today = LocalDate.now();

        for (GridCellInfo cell : gridCells) {

            try {
                LOGGER.debug("Fetching weather for Lat={}, Lon={}", cell.getTargetLatitude(), cell.getTargetLongitude());

                double lat = cell.getTargetLatitude();
                double lon = cell.getTargetLongitude();

                WeatherReportDTO report = weatherService.getWeather(null, lon, lat, today);

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
                LOGGER.warn("Could not fetch temperature data for point {}, {}",
                        cell.getTargetLatitude(), cell.getTargetLongitude(), e);
            }
        }

        return gridTemperatures;
    }

    public List<GridTemperature> getAllTemperatureGridPoints() {
        List<GridTemperature> allPoints = new ArrayList<>();

        for (String state : austrianStates.keySet()) {
            List cached = cacheManager.getCache("temperatureGrid")
                    .get(state, List.class);
            if (cached != null) {
                allPoints.addAll(cached);
            } else {
                LOGGER.warn("No cached temperature grid for state: {}", state);
            }
        }
        return allPoints;

    }

    @PreDestroy
    public void shutdown() {
        executorService.shutdown();
    }

}
