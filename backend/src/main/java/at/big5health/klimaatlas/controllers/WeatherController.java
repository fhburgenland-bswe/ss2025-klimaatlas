package at.big5health.klimaatlas.controllers;

import at.big5health.klimaatlas.config.AustrianPopulationCenter;
import at.big5health.klimaatlas.dtos.WeatherReportDTO;
import at.big5health.klimaatlas.grid.GridTemperature;
import at.big5health.klimaatlas.services.WeatherService;
import lombok.AllArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.cache.CacheManager;
import org.springframework.cache.Cache;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/**
 * REST controller for retrieving weather-related information.
 * <p>
 * This controller provides endpoints to fetch daily weather reports and
 * temperature grid data. It is currently configured to allow cross-origin requests
 * from any domain ({@code @CrossOrigin("*")}).
 * All endpoints are relative to the base path {@code /dailyweather}.
 * <p>
 * Dependencies are injected via constructor using Lombok's {@code @AllArgsConstructor}.
 *
 * @see WeatherService
 * @see WeatherReportDTO
 * @see GridTemperature
 * @since 1.0.0
 */
@RestController
@RequestMapping("/dailyweather")
@CrossOrigin("*") // Allows all origins
@AllArgsConstructor // Lombok annotation for constructor injection
public class WeatherController {

    private final WeatherService weatherService;

    private final CacheManager cacheManager;
    // No explicit constructor needed due to @AllArgsConstructor.

    /**
     * Retrieves the daily weather report for a specified city located at these coordinates, and date.
     * <p>
     * This endpoint requires the city name (although it is currently not used),
     * longitude, latitude, and the specific date for which the weather
     * report is requested. The date must be in ISO date format
     * (YYYY-MM-DD).
     *
     * @param cityName The name of the city for which to fetch the weather.
     *                 Must not be empty. But currently not used since we switched to geocoding in the frontend.
     * @param longitude The longitude of the location (e.g., 16.3738).
     *                 Must be a valid Double.
     * @param latitude The latitude of the location (e.g., 48.2082).
     *                 Must be a valid Double.
     * @param actualDate The specific date for the weather report, formatted as YYYY-MM-DD.
     *                 Must be a valid LocalDate.
     * @return A {@link ResponseEntity} containing the {@link WeatherReportDTO}
     *         and HTTP status 200 (OK) on success.
     *         May return HTTP 400 (Bad Request) if parameters are invalid/missing,
     *         or HTTP 404 (Not Found) if weather data cannot be retrieved.
     * @see WeatherService#getWeather(String, Double, Double, LocalDate)
     * @see WeatherReportDTO
     */
    @GetMapping
    @ResponseStatus(HttpStatus.OK) // Explicitly states the success status
    public ResponseEntity<WeatherReportDTO> getWeather(
            @RequestParam String cityName, // Spring automatically makes this required
            @RequestParam Double longitude,
            @RequestParam Double latitude,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate actualDate
    ) {
        WeatherReportDTO report = weatherService.getWeather(cityName, longitude, latitude, actualDate);
        return ResponseEntity.ok(report);
    }

    /**
     * Retrieves cached weather data for all predefined Austrian population centers for a specific date.
     * <p>
     * This endpoint returns a list of {@link WeatherReportDTO} objects representing weather information
     * for each location defined in the {@link AustrianPopulationCenter} enum. It directly accesses the
     * Spring cache named {@code "weatherCache"} and does not trigger any external API calls or data fetching
     * via service methods.
     * <p>
     * The method guarantees that only fully cached datasets are returned. If any of the required
     * entries are missing from the cache, the endpoint will return {@code 204 No Content} to indicate that
     * a complete dataset is not yet available.
     * <p>
     * Typical use case: display pre-cached weather data (e.g., pins on a map) on the frontend without
     * risking API latency or failures.
     *
     * @param actualDate The date for which the cached weather data is requested, in ISO format (YYYY-MM-DD).
     *                   Typically this should match the date used in the application's scheduled or startup
     *                   pre-caching (usually yesterday).
     * @return A {@link ResponseEntity} containing:
     *         <ul>
     *             <li>HTTP 200 (OK) and a full list of {@link WeatherReportDTO} if all data is found in the cache.</li>
     *             <li>HTTP 204 (No Content) if any city is missing from the cache.</li>
     *             <li>HTTP 500 (Internal Server Error) if the cache could not be accessed.</li>
     *         </ul>
     *
     * @see WeatherReportDTO
     * @see AustrianPopulationCenter
     * @see org.springframework.cache.CacheManager
     */
    @GetMapping("/cached")
    public ResponseEntity<List<WeatherReportDTO>> getAllCachedWeatherData(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate actualDate) {

        List<WeatherReportDTO> results = new ArrayList<>();
        Cache cache = cacheManager.getCache("weatherCache");

        if (cache == null) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }

        for (AustrianPopulationCenter center : AustrianPopulationCenter.values()) {
            String key = center.getRepresentativeLatitude() + "_" + center.getRepresentativeLongitude() + "_" + actualDate;
            WeatherReportDTO cached = cache.get(key, WeatherReportDTO.class);

            if (cached == null) {
                return ResponseEntity.status(HttpStatus.NO_CONTENT).build();
            }

            cached.setCityName(center.getDisplayName());

            results.add(cached);
        }

        return ResponseEntity.ok(results);
    }


    /**
     * Retrieves temperature grid data points, optionally filtered by state.
     * <p>
     * If a 'state' query parameter is provided and is not empty, the grid points
     * will be filtered for that specific state. Otherwise, all available temperature
     * grid points are returned.
     *
     * @param state An optional query parameter representing the state (e.g., "Tyrol", "Vienna")
     *              to filter the temperature grid points. If null or empty, all points are returned.
     * @return A {@link ResponseEntity} containing a list of
     *         {@link GridTemperature} objects and HTTP status 200 (OK).
     *         The list may be empty if no data is found for the given criteria.
     *         May return HTTP 404 (Not Found) if data for a specific state is requested but not found.
     * @see GridCacheService#getTemperatureGridForState(String)
     * @see GridCacheService#getAllTemperatureGridPoints()
     * @see GridTemperature
     */
//    @GetMapping("/temperature-grid")
//    @ResponseStatus(HttpStatus.OK) //is implicit with ResponseEntity.ok() but can be added for clarity
//    public ResponseEntity<List<GridTemperature>> getTemperatureGridPoints(
//            @RequestParam(required = false) String state) { // 'required = false' makes it optional
//
//        List<GridTemperature> gridPoints;
//
//        if (state != null && !state.isEmpty()) {
//            gridPoints = gridCacheService.getTemperatureGridForState(state);
//        } else {
//            gridPoints = gridCacheService.getAllTemperatureGridPoints();
//        }
//
//        return ResponseEntity.ok(gridPoints);
//    }

}
