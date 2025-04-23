package at.big5health.klimaatlas.dtos;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class WeatherReportDTO {
    private Double minTemp;
    private Double maxTemp;
    private Precipitation precip;
    private Double sunDuration;
    private Double latitude;
    private Double longitude;
}
