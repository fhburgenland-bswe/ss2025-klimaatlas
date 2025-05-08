package at.big5health.klimaatlas.dtos;

import at.big5health.klimaatlas.models.WeatherReport;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Data Transfer Object (DTO) representing a weather report.
 * <p>
 * This DTO is typically used to transfer weather report data to clients,
 * for example, as a response body in API endpoints. Its structure mirrors
 * the {@link WeatherReport} model.
 * <p>
 * Lombok annotations {@link Data @Data}, {@link AllArgsConstructor @AllArgsConstructor},
 * and {@link NoArgsConstructor @NoArgsConstructor} are used to generate
 * standard boilerplate code like getters, setters, constructors,
 * {@code toString()}, {@code equals()}, and {@code hashCode()}.
 *
 * @see at.big5health.klimaatlas.controllers.WeatherController
 * @see at.big5health.klimaatlas.services.WeatherService
 * @see WeatherReport
 * @see Precipitation
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class WeatherReportDTO {

    /**
     * The minimum temperature recorded, in degrees Celsius.
     */
    private Double minTemp;

    /**
     * The maximum temperature recorded, in degrees Celsius.
     */
    private Double maxTemp;

    /**
     * The type of precipitation observed.
     * Values correspond to classifications from the Geosphere SPARTACUS api.
     *
     * @see Precipitation
     */
    private Precipitation precip;

    /**
     * The duration of sunshine, typically measured in hours.
     */
    private Double sunDuration;

    /**
     * The geographical latitude of the location for the weather report.
     * Expressed in decimal degrees.
     */
    private Double latitude;

    /**
     * The geographical longitude of the location for the weather report.
     * Expressed in decimal degrees.
     */
    private Double longitude;
}
