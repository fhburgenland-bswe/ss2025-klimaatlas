package at.big5health.klimaatlas.services; // Ensure this package matches yours

import at.big5health.klimaatlas.config.AustrianPopulationCenter; // Ensure this import is correct
import at.big5health.klimaatlas.exceptions.ExternalApiException;
import at.big5health.klimaatlas.exceptions.WeatherDataNotFoundException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.List;

/**
 * Service responsible for pre-caching weather data for all configured Austrian population centers.
 * Weather data is fetched from the external API and cached during application startup
 * and as a scheduled daily job. Pre-caching ensures that common queries (e.g. for the previous day)
 * are fast and avoid unnecessary API calls from the frontend.
 * This class depends on {@link WeatherService} for data retrieval and
 * {@link PopulationCenterService} for loading the list of cities.
 */
@Service
public class WeatherPreCacheService {

    private static final Logger LOG = LoggerFactory.getLogger(WeatherPreCacheService.class);

    private final WeatherService weatherService;

    private final PopulationCenterService populationCenterService;

    public WeatherPreCacheService(
            WeatherService weatherService,
            PopulationCenterService populationCenterService) {
        this.weatherService = weatherService;
        this.populationCenterService = populationCenterService;
    }

    /**
     * Triggers the pre-caching process once the application is fully started.
     * This runs asynchronously to avoid blocking application startup.
     */
    @EventListener(ApplicationReadyEvent.class)
    @Async
    public void preCacheOnStartup() {
        LOG.info("Application ready. Starting initial pre-cache of weather data for population centers (asynchronously)...");
        performPreCaching("Startup");
        LOG.info("Initial pre-cache task submitted/completed (asynchronously).");
    }

    /**
     * Runs daily at 10:00 CET and pre-caches weather data for all population centers.
     * Intended to refresh cached data with up-to-date results.
     */
    @Scheduled(cron = "0 0 10 * * *", zone = "CET")
    public void scheduledPreCache() {
        ZonedDateTime cetTime = ZonedDateTime.now(ZoneId.of("CET"));
        LOG.info("Scheduled pre-cache triggered at {} CET. Starting daily pre-cache of weather data...", cetTime);
        performPreCaching("Scheduled");
        LOG.info("Daily pre-cache completed at {} CET.", ZonedDateTime.now(ZoneId.of("CET")));
    }

    /**
     * Internal method that performs the actual pre-caching logic.
     * Fetches weather data for all centers for the previous day and stores results in the cache.
     *
     * @param triggerSource a label indicating whether this was called by "Startup", "Scheduled", etc.
     */
    public void performPreCaching(String triggerSource) {
        LocalDate dateToFetch = LocalDate.now().minusDays(1);
        LOG.info("[{}] Pre-caching weather data for date: {}", triggerSource, dateToFetch);

        int successCount = 0;
        int failureCount = 0;
        long delayBetweenRequestsMs = 250;

        List<AustrianPopulationCenter> centers = populationCenterService.getAllCenters();

        for (AustrianPopulationCenter center : centers) {
            LOG.debug("[{}] Attempting to pre-cache data for: {} using representative point (Lat:{}, Lon:{}) on {}",
                    triggerSource, center.getDisplayName(), center.getRepresentativeLatitude(), center.getRepresentativeLongitude(), dateToFetch);
            try {
                // USE THE NEW REPRESENTATIVE COORDINATES
                weatherService.getWeather(
                        center.getDisplayName(),
                        center.getRepresentativeLongitude(), // Use representative longitude
                        center.getRepresentativeLatitude(),  // Use representative latitude
                        dateToFetch
                );
                LOG.info("[{}] Successfully pre-cached data for: {}", triggerSource, center.getDisplayName());
                successCount++;
            } catch (WeatherDataNotFoundException e) {
                LOG.warn("[{}] Weather data not found during pre-cache for {}: {}", triggerSource, center.getDisplayName(), e.getMessage());
                failureCount++;
            } catch (ExternalApiException e) {
                LOG.error("[{}] External API error pre-caching data for {}: {}", triggerSource, center.getDisplayName(), e.getMessage());
                failureCount++;
            } catch (Exception e) {
                LOG.error("[{}] Unexpected error pre-caching data for {}: {}", triggerSource, center.getDisplayName(), e.getMessage(), e);
                failureCount++;
            }

            try {
                Thread.sleep(delayBetweenRequestsMs);
            } catch (InterruptedException e) {
                LOG.warn("[{}] Pre-cache thread interrupted. Stopping pre-cache.", triggerSource);
                Thread.currentThread().interrupt();
                break;
            }
        }
        LOG.info("[{}] Pre-caching summary: {} successes, {} failures.", triggerSource, successCount, failureCount);
    }
}
