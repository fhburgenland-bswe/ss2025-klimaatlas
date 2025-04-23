package at.big5health.klimaatlas.models;

import at.big5health.klimaatlas.dtos.Precipitation;
import lombok.Data;

@Data
public class WeatherReport {
    private Double minTemp;
    private Double maxTemp;
    private Precipitation precip;
    private Double sunDuration;
    private Double latitude;
    private Double longitude;
}
