package at.big5health.klimaatlas.config; // Or your config package

import java.util.Locale;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class AustrianPopulationCenter {
    // Values: displayName, representativeLat, representativeLon, minLat, minLon, maxLat, maxLon
    // Bounding boxes are wider area approximations.
    private String displayName;
    private double representativeLatitude;  // New field for the single point
    private double representativeLongitude; // New field for the single point
    private double minLatitude;             // Existing bounding box fields
    private double minLongitude;
    private double maxLatitude;
    private double maxLongitude;

    // Lombok's @RequiredArgsConstructor will generate the constructor for all final fields.
    // Lombok's @Getter will generate getters for all fields.

    /**
     * Returns the bounding box as a string formatted for the API.
     * Useful if you ever need to request data for the entire bounding box.
     * Format: "min_lat,min_lon,max_lat,max_lon"
     */
    public String getBboxString() {
        return String.format(
                Locale.US,
                "%.6f,%.6f,%.6f,%.6f",
                minLatitude,
                minLongitude,
                maxLatitude,
                maxLongitude
        );
    }
}
