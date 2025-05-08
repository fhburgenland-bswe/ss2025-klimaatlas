package at.big5health.klimaatlas.models;

import at.big5health.klimaatlas.dtos.Precipitation;
import lombok.Data;

/**
 * Represents a structured weather report for a specific location and time.
 * <p>
 * This class encapsulates various meteorological data points such as temperature,
 * type of precipitation (as defined by the {@link Precipitation} enum, which maps
 * to Geosphere SPARTACUS response fields), and sunshine duration. It currently
 * serves as a core data model within the application and represents the Response
 * Object returned to the Frontend Weather Report
 * <p>
 * The {@link lombok.Data} annotation automatically generates standard boilerplate code.
 *
 * @see at.big5health.klimaatlas.services.WeatherService
 * @see at.big5health.klimaatlas.dtos.WeatherReportDTO
 * @see at.big5health.klimaatlas.dtos.Precipitation
 */
@Data
public class WeatherReport {

    /**
     * The minimum temperature recorded for the day, in degrees Celsius.
     */
    private Double minTemp;

    /**
     * The maximum temperature recorded for the day, in degrees Celsius.
     */
    private Double maxTemp;

    /**
     * The type of precipitation observed during the period, represented by the
     * {@link Precipitation} enum. These enum values correspond to
     * classifications received from the Geosphere SPARTACUS api.
     *
     * @see at.big5health.klimaatlas.dtos.Precipitation
     */
    private Precipitation precip;

    /**
     * The duration of sunshine, typically measured in hours.
     */
    private Double sunDuration;

    /**
     * The geographical latitude of the location for which this weather report applies.
     * Expressed in decimal degrees.
     */
    private Double latitude;

    /**
     * The geographical longitude of the location for which this weather report applies.
     * Expressed in decimal degrees.
     */
    private Double longitude;

}
