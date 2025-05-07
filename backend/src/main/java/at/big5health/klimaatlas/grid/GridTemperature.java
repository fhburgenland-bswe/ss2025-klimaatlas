package at.big5health.klimaatlas.grid;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class GridTemperature {

    private final double latitude;
    private final double longitude;
    private final double temperature;

}
