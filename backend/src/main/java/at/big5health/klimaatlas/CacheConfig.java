package at.big5health.klimaatlas;

import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.concurrent.ConcurrentMapCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;
import java.util.Arrays;

@Configuration
@EnableCaching
@EnableScheduling
public class CacheConfig {

    @Bean
    public CacheManager cacheManager() {
        ConcurrentMapCacheManager manager = new ConcurrentMapCacheManager();
        manager.setAllowNullValues(false);

        // Cache-Regionen definieren:
        // - weatherCache: Für individuelle Wetterdatenabfragen
        // - temperatureGrid: Für das Temperatur-Raster pro Bundesland
        manager.setCacheNames(Arrays.asList("weatherCache", "temperatureGrid"));

        return manager;
    }

    @Bean
    public TaskScheduler taskScheduler() {
        ThreadPoolTaskScheduler scheduler = new ThreadPoolTaskScheduler();
        scheduler.setPoolSize(5); // Anzahl der Threads für parallele geplante Aufgaben
        scheduler.setThreadNamePrefix("scheduled-task-");
        scheduler.setErrorHandler(t -> {
            // Fehlerbehandlung für geplante Aufgaben
            System.err.println("Error in scheduled task: " + t.getMessage());
            t.printStackTrace();
        });
        return scheduler;
    }

}
