package at.big5health.klimaatlas.controllers;

import at.big5health.klimaatlas.config.AustrianPopulationCenter;
import at.big5health.klimaatlas.dtos.WeatherReportDTO;
import at.big5health.klimaatlas.grid.GridTemperature;
import at.big5health.klimaatlas.services.PopulationCenterService;
import at.big5health.klimaatlas.services.WeatherService;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
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
@Tag(name = "Weather", description = "API for the daily weather report")
public class WeatherController {

    private final WeatherService weatherService;

    private final CacheManager cacheManager;

    private final PopulationCenterService populationCenterService;
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
    @ApiResponse(responseCode = "200", description = "Success status")
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
     * Returns cached weather data for all configured Austrian population centers on a specific date.
     * The data is read directly from the {@code weatherCache} without triggering external API calls.
     * Only fully cached datasets are returned — if any entry is missing, a {@code 204 No Content} is returned.
     * Typical usage: frontend loads this on startup to display preloaded weather data on the map.
     *
     * @param actualDate the date for which cached weather data is requested (ISO format: YYYY-MM-DD)
     * @return 200 OK with list of {@link WeatherReportDTO} if all entries are found in cache,
     *         204 No Content if any are missing,
     *         or 500 Internal Server Error if cache access fails
     */
    @GetMapping("/cached")
    @ApiResponse(responseCode = "200", description = "Success status")
    public ResponseEntity<List<WeatherReportDTO>> getAllCachedWeatherData(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate actualDate) {

        List<WeatherReportDTO> results = new ArrayList<>();
        Cache cache = cacheManager.getCache("weatherCache");

        if (cache == null) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }

        List<AustrianPopulationCenter> centers = populationCenterService.getAllCenters();

        for (AustrianPopulationCenter center : centers) {
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
}
