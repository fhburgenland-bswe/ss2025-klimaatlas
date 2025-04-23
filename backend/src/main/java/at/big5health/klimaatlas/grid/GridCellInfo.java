package at.big5health.klimaatlas.grid;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class GridCellInfo {
    private String cellId;
    private BoundingBox bbox;
    private double targetLatitude;
    private double targetLongitude;
}
