package at.big5health.klimaatlas.controllers;

import at.big5health.klimaatlas.dtos.WeatherReportDTO;
import at.big5health.klimaatlas.services.GridCacheService;
import at.big5health.klimaatlas.services.WeatherService;
import lombok.AllArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.ResponseStatus;

import java.time.LocalDate;
import java.util.List;


@RestController
@RequestMapping("/dailyweather")
@CrossOrigin("*")
@AllArgsConstructor
public class WeatherController {

    private final WeatherService weatherService;
    private final GridCacheService gridCacheService;

    @GetMapping
    @ResponseStatus(HttpStatus.OK)
    public ResponseEntity<WeatherReportDTO> getWeather(
            @RequestParam  String cityName,
            @RequestParam  Double longitude,
            @RequestParam  Double latitude,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate actualDate
    ) {
        WeatherReportDTO report = weatherService.getWeather(cityName, longitude, latitude, actualDate);
        return ResponseEntity.ok(report);
    }

    @GetMapping("/temperature-grid")
    public ResponseEntity<List<GridCacheService.GridTemperature>> getTemperatureGridPoints(
            @RequestParam(required = false) String state) {

        List<GridCacheService.GridTemperature> gridPoints;

        if (state != null && !state.isEmpty()) {
            gridPoints = gridCacheService.getTemperatureGridForState(state);
        } else {
            gridPoints = gridCacheService.getAllTemperatureGridPoints();
        }

        return ResponseEntity.ok(gridPoints);
    }

}
