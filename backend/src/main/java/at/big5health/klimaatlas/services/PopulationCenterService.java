package at.big5health.klimaatlas.services;

import at.big5health.klimaatlas.config.AustrianPopulationCenter;
import at.big5health.klimaatlas.Components.PopulationCenterLoader;
import jakarta.annotation.PostConstruct;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import java.util.stream.Collectors;

/**
 * Service responsible for managing Austrian population center data.
 * Loads and keeps in memory the list of centers defined in an external CSV file.
 * Provides access to these centers and supports reloading and selective re-caching
 * of weather data when changes are detected.
 */
@Service
public class PopulationCenterService {

    private List<AustrianPopulationCenter> centers = new ArrayList<>();

    private List<String> lastCsvErrors = new ArrayList<>();

    private final PopulationCenterLoader populationCenterLoader;

    public PopulationCenterService(PopulationCenterLoader populationCenterLoader) {
        this.populationCenterLoader = populationCenterLoader;
    }

    /**
     * Loads population centers on application startup.
     * If loading fails, an empty list is retained and the error is logged.
     */
    @PostConstruct
    public void init() {
        try {
            this.centers = populationCenterLoader.loadFromCSV();
        } catch (RuntimeException e) {
            System.err.println("Failed to load centers: " + e.getMessage());
        }
    }

    /**
     * Returns the list of loaded population centers.
     * If the last CSV load resulted in validation errors, those are re-thrown here.
     *
     * @return list of loaded {@link AustrianPopulationCenter} instances
     * @throws at.big5health.klimaatlas.exceptions.CsvParseException if previous load had errors
     */
    public List<AustrianPopulationCenter> getAllCenters() {
        if (!lastCsvErrors.isEmpty()) {
            throw new at.big5health.klimaatlas.exceptions.CsvParseException(lastCsvErrors);
        }

        return centers;
    }

    /**
     * Reloads the CSV and re-caches weather data for any newly added population centers.
     * In case of parsing errors, the previously loaded list is kept and error messages are stored.
     *
     * @param actualDate the date for which to re-cache weather data
     * @param reCacheFunction function that re-caches one {@link AustrianPopulationCenter}
     */
    public void refreshAndReCache(LocalDate actualDate, Consumer<AustrianPopulationCenter> reCacheFunction) {
        try {
            List<AustrianPopulationCenter> newCenters = populationCenterLoader.loadFromCSV();

            List<AustrianPopulationCenter> toBeReCached = newCenters.stream()
                    .filter(newCenter ->
                            centers.stream().noneMatch(old ->
                                    old.getDisplayName().equalsIgnoreCase(newCenter.getDisplayName())
                                            && old.getRepresentativeLatitude() == newCenter.getRepresentativeLatitude()
                                            && old.getRepresentativeLongitude() == newCenter.getRepresentativeLongitude()
                            )
                    )
                    .collect(Collectors.toList());

            this.centers = newCenters;
            this.lastCsvErrors = List.of();

            toBeReCached.forEach(reCacheFunction);

        } catch (RuntimeException e) {
            if (e instanceof at.big5health.klimaatlas.exceptions.CsvParseException csvEx) {
                this.lastCsvErrors = csvEx.getErrors();
            } else {
                this.lastCsvErrors = List.of(e.getMessage());
            }
            throw e;
        }
    }
}