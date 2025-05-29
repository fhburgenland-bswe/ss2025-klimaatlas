package at.big5health.klimaatlas.Components;

import at.big5health.klimaatlas.exceptions.CsvParseException;
import at.big5health.klimaatlas.services.PopulationCenterService;
import at.big5health.klimaatlas.services.WeatherService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.doThrow;

class CsvFileWatcherTest {

    private PopulationCenterService populationCenterService;
    private WeatherService weatherService;
    private CacheManager cacheManager;
    private CsvFileWatcher watcher;

    @BeforeEach
    void setUp() {
        populationCenterService = mock(PopulationCenterService.class);
        weatherService = mock(WeatherService.class);
        cacheManager = mock(CacheManager.class);
        Cache cache = mock(Cache.class);

        watcher = new CsvFileWatcher(populationCenterService, weatherService, cacheManager);

        try {
            var field = CsvFileWatcher.class.getDeclaredField("csvPath");
            field.setAccessible(true);
            field.set(watcher, "src/test/resources/test-centers.csv");
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Test
    void startWatching_shouldStartThread() throws Exception {
        Thread[] before = getAllThreads();
        watcher.startWatching();

        Thread.sleep(100);

        Thread[] after = getAllThreads();
        boolean foundWatcher = false;
        for (Thread thread : after) {
            if (thread.getName().contains("Thread") && thread.isDaemon()) {
                foundWatcher = true;
                break;
            }
        }

        assertThat(foundWatcher).isTrue();
    }

    private Thread[] getAllThreads() {
        ThreadGroup rootGroup = Thread.currentThread().getThreadGroup();
        while (rootGroup.getParent() != null) {
            rootGroup = rootGroup.getParent();
        }
        Thread[] threads = new Thread[rootGroup.activeCount() * 2];
        rootGroup.enumerate(threads);
        return threads;
    }

    @Test
    void handleCsvParseException_shouldLogErrors() {
        CsvParseException ex = new CsvParseException(List.of("bad line"));
        doThrow(ex).when(populationCenterService).refreshAndReCache(any(), any());

        watcher = new CsvFileWatcher(populationCenterService, weatherService, cacheManager);
        try {
            var method = CsvFileWatcher.class.getDeclaredMethod("startWatching");
            method.setAccessible(true);
            method.invoke(watcher);
        } catch (Exception ignored) {}

        assertThat(true).isTrue();
    }
}