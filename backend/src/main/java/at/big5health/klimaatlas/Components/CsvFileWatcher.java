package at.big5health.klimaatlas.Components;

import at.big5health.klimaatlas.dtos.WeatherReportDTO;
import at.big5health.klimaatlas.exceptions.CsvParseException;
import at.big5health.klimaatlas.services.PopulationCenterService;
import at.big5health.klimaatlas.services.WeatherService;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.file.FileSystems;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.WatchService;
import java.nio.file.StandardWatchEventKinds;
import java.nio.file.WatchKey;
import java.nio.file.WatchEvent;
import java.time.LocalDate;

/**
 * Component that watches the population center CSV file for changes.
 * If the file is modified, it triggers a reload of the data and selectively re-caches weather info
 * for any newly added centers.
 * Parsing errors (e.g. invalid rows or duplicates) are logged and stored, but do not crash the app.
 * Starts automatically at application startup using a background daemon thread.
 */
@Component
@RequiredArgsConstructor
public class CsvFileWatcher {

    private final PopulationCenterService populationCenterService;
    private final WeatherService weatherService;
    private final CacheManager cacheManager;

    @Value("${population.centers.csv-path}")
    private String csvPath;

    /**
     * Initializes file watching on the configured CSV path.
     * Triggers population center refresh and re-caching if the file changes.
     */
    @PostConstruct
    public void startWatching() {
        Thread watcherThread = new Thread(() -> {
            try {
                Path filePath = Paths.get(csvPath).toAbsolutePath();
                Path dir = filePath.getParent();
                String fileName = filePath.getFileName().toString();

                WatchService watchService = FileSystems.getDefault().newWatchService();
                dir.register(watchService, StandardWatchEventKinds.ENTRY_MODIFY);

                System.out.println("[CSV Watcher] Watching for changes in: " + filePath);

                while (true) {
                    WatchKey key = watchService.take();
                    for (WatchEvent<?> event : key.pollEvents()) {
                        Path changed = (Path) event.context();

                        if (event.kind() == StandardWatchEventKinds.ENTRY_MODIFY &&
                                changed.getFileName().toString().equals(fileName)) {

                            System.out.println("[CSV Watcher] Detected change in file: " + fileName);

                            try {
                                LocalDate targetDate = LocalDate.now().minusDays(1);
                                Cache cache = cacheManager.getCache("weatherCache");

                                if (cache == null) {
                                    System.err.println("[CSV Watcher] Cache 'weatherCache' not found!");
                                    continue;
                                }

                                populationCenterService.refreshAndReCache(targetDate, center -> {
                                    try {
                                        WeatherReportDTO report = weatherService.getWeather(
                                                center.getDisplayName(),
                                                center.getRepresentativeLongitude(),
                                                center.getRepresentativeLatitude(),
                                                targetDate
                                        );
                                        String keyStr = center.getRepresentativeLatitude() + "_" +
                                                center.getRepresentativeLongitude() + "_" +
                                                targetDate;
                                        cache.put(keyStr, report);
                                        System.out.println("[CSV Watcher] Re-cached weather for: " + center.getDisplayName());
                                        Thread.sleep(1000);
                                    } catch (InterruptedException e) {
                                        Thread.currentThread().interrupt();
                                        System.err.println("[CSV Watcher] Interrupted during delay.");
                                    } catch (Exception ex) {
                                        System.err.println("[CSV Watcher] Failed to re-cache: " + center.getDisplayName() + " → " + ex.getMessage());
                                    }
                                });

                            } catch (CsvParseException ex) {
                                System.err.println("[CSV Watcher] Parsing error: " + ex.getErrors());
                            }
                        }
                    }
                    key.reset();
                }

            } catch (IOException | InterruptedException e) {
                System.err.println("[CSV Watcher] Error: " + e.getMessage());
            }
        });

        watcherThread.setDaemon(true);
        watcherThread.start();
    }
}