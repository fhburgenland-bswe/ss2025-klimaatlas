package at.big5health.klimaatlas.config; // Or your config package

import java.util.Locale;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.ToString;

@Getter
@RequiredArgsConstructor // Lombok will automatically include new final fields in constructor
@ToString(of = "displayName")
public enum AustrianPopulationCenter {
    // Values: displayName, representativeLat, representativeLon, minLat, minLon, maxLat, maxLon
    // Representative coordinates are approximations for city centers.
    // Bounding boxes are wider area approximations.

    // State Capitals
    VIENNA("Vienna (Wien)",             48.2082, 16.3738,  48.1200, 16.1800, 48.3300, 16.5800), // Stephansplatz approx.
    GRAZ("Graz",                       47.0707, 15.4395,  46.9900, 15.3500, 47.1200, 15.5200), // Hauptplatz approx.
    LINZ("Linz",                       48.3069, 14.2858,  48.2200, 14.1800, 48.3700, 14.4000), // Hauptplatz approx.
    SALZBURG("Salzburg",                 47.8095, 13.0550,  47.7500, 12.9800, 47.8500, 13.1200), // Old Town center approx.
    INNSBRUCK("Innsbruck",                47.2692, 11.4041,  47.2200, 11.3000, 47.3200, 11.5000), // Golden Roof approx.
    KLAGENFURT("Klagenfurt am Wörthersee",46.6247, 14.3050,  46.5800, 14.2000, 46.6800, 14.4000), // Neuer Platz approx.
    ST_POELTEN("St. Pölten",             48.2047, 15.6256,  48.1500, 15.5500, 48.2500, 15.7000), // Rathausplatz approx.
    BREGENZ("Bregenz",                  47.5031, 9.7471,   47.4700, 9.6800, 47.5300, 9.8000),  // Harbor area approx.
    EISENSTADT("Eisenstadt",             47.8456, 16.5267,  47.8200, 16.4500, 47.8800, 16.5800), // Schloss Esterházy approx.

    // Other Significant Cities
    WIENER_NEUSTADT("Wiener Neustadt",   47.8139, 16.2436,  47.7700, 16.1500, 47.8700, 16.3000), // Hauptplatz approx.
    WELS("Wels",                         48.1575, 14.0250,  48.1200, 13.9500, 48.2000, 14.1000), // Stadtplatz approx.
    VILLACH("Villach",                   46.6125, 13.8469,  46.5700, 13.7500, 46.6700, 13.9500), // Hauptplatz approx.
    DORNBIRN("Dornbirn",                47.4125, 9.7439,   47.3500, 9.6800, 47.4500, 9.8200),  // Marktplatz approx.
    STEYR("Steyr",                     48.0386, 14.4219,  48.0000, 14.3500, 48.0800, 14.4800),   // Stadtplatz approx.
    OBRWART("Oberwart",                47.2897, 16.2058,  47.2500, 16.1500, 47.3100, 16.2500);  // Hauptplatz approx.

    private final String displayName;
    private final double representativeLatitude;  // New field for the single point
    private final double representativeLongitude; // New field for the single point
    private final double minLatitude;             // Existing bounding box fields
    private final double minLongitude;
    private final double maxLatitude;
    private final double maxLongitude;

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
