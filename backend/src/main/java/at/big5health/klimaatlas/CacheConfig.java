package at.big5health.klimaatlas;

import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.concurrent.ConcurrentMapCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;
import org.springframework.util.ErrorHandler; // For TaskScheduler ErrorHandler

import java.util.Arrays;

/**
 * Spring configuration class for caching and scheduling functionalities.
 * <p>
 * This class enables Spring's caching abstraction via {@link EnableCaching @EnableCaching}
 * and task scheduling capabilities via {@link EnableScheduling @EnableScheduling}.
 * It defines beans for the {@link CacheManager} and {@link TaskScheduler}
 * used throughout the application.
 *
 * @see EnableCaching
 * @see EnableScheduling
 * @see CacheManager
 * @see TaskScheduler
 */
@Configuration
@EnableCaching
@EnableScheduling
public class CacheConfig {

    /**
     * Defines the primary {@link CacheManager} bean for the application.
     * <p>
     * This configuration creates a {@link ConcurrentMapCacheManager}, which uses
     * simple {@link java.util.concurrent.ConcurrentHashMap ConcurrentHashMaps} as
     * the underlying cache stores.
     * It is configured with predefined cache names: "weatherCache" and "temperatureGrid".
     * Null values are not permitted in the cache ({@code setAllowNullValues(false)}).
     *
     * @return A configured {@link ConcurrentMapCacheManager} instance.
     * @see ConcurrentMapCacheManager
     * @see at.big5health.klimaatlas.services.WeatherService (uses "weatherCache")
     * @see at.big5health.klimaatlas.services.GridCacheService (uses "temperatureGrid")
     */
    @Bean
    public CacheManager cacheManager() {
        ConcurrentMapCacheManager manager = new ConcurrentMapCacheManager();
        // Disallow null values to be stored in the cache.
        // This can help prevent issues if methods return null and caching is conditional (e.g., unless="#result == null").
        manager.setAllowNullValues(false);

        // Predefine the cache names used in the application.
        // This helps in managing and initializing caches.
        manager.setCacheNames(Arrays.asList("weatherCache", "temperatureGrid"));
        // Other caches: "dailyWeatherDataGrid" was also mentioned in WeatherService.
        // Consider adding it here if it's a distinct cache managed by this CacheManager.
        // If "dailyWeatherDataGrid" is intended to be the same as "weatherCache" or "temperatureGrid",
        // ensure consistency in naming.

        return manager;
    }

    /**
     * Defines the {@link TaskScheduler} bean for managing scheduled tasks.
     * <p>
     * This configuration creates a {@link ThreadPoolTaskScheduler} with a
     * configurable pool size and thread name prefix. It also includes a basic
     * error handler that logs exceptions from scheduled tasks to standard error.
     *
     * @return A configured {@link ThreadPoolTaskScheduler} instance.
     * @see ThreadPoolTaskScheduler
     * @see EnableScheduling
     * @see at.big5health.klimaatlas.services.GridCacheService#refreshCache() (example of a scheduled task)
     */
    @Bean
    public TaskScheduler taskScheduler() {
        ThreadPoolTaskScheduler scheduler = new ThreadPoolTaskScheduler();
        scheduler.setPoolSize(5); // Configures the number of threads for scheduled tasks
        scheduler.setThreadNamePrefix("scheduled-task-"); // Sets a prefix for thread names for easier identification
        scheduler.setErrorHandler(new ErrorHandler() { // Basic error handler for scheduled tasks
            @Override
            public void handleError(Throwable t) {
                // In a production application, use a proper logger (e.g., SLF4J)
                System.err.println("Error occurred in scheduled task: " + t.getMessage());
                t.printStackTrace(System.err); // Print stack trace to standard error
            }
        });
        return scheduler;
    }
}
