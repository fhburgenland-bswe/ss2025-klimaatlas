package at.big5health.klimaatlas.grid;

import lombok.AllArgsConstructor;
import lombok.Getter;
import java.util.Locale;

@Getter
@AllArgsConstructor
public class BoundingBox {
    private double minLat;
    private double minLon;
    private double maxLat;
    private double maxLon;

    // Format for the API call using Locale.US for dot decimal separator
    public String toApiString() {
        return String.format(Locale.US, "%.6f,%.6f,%.6f,%.6f", minLat, minLon, maxLat, maxLon);
    }
}
