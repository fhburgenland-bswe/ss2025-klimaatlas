package at.big5health.klimaatlas.grid;

import lombok.AllArgsConstructor;
import lombok.Getter;
import java.util.Locale;

/**
 * Represents a geographical bounding box defined by minimum and maximum
 * latitude and longitude coordinates.
 * <p>
 * This class is typically used to define a rectangular area on a map.
 * It provides a method to format its coordinates into a string suitable
 * for API calls.
 * Lombok's {@link Getter @Getter} and {@link AllArgsConstructor @AllArgsConstructor}
 * are used for boilerplate code generation.
 *
 * @see GridUtil
 * @see GridCellInfo
 */
@Getter
@AllArgsConstructor
public class BoundingBox {

    /**
     * The minimum latitude of the bounding box, in decimal degrees.
     * Represents the southern boundary.
     */
    private double minLat;

    /**
     * The minimum longitude of the bounding box, in decimal degrees.
     * Represents the western boundary.
     */
    private double minLon;

    /**
     * The maximum latitude of the bounding box, in decimal degrees.
     * Represents the northern boundary.
     */
    private double maxLat;

    /**
     * The maximum longitude of the bounding box, in decimal degrees.
     * Represents the eastern boundary.
     */
    private double maxLon;

    /**
     * Formats the bounding box coordinates into a string suitable for API calls.
     * <p>
     * The format is "minLat,minLon,maxLat,maxLon" with coordinates formatted
     * to six decimal places, using a dot as the decimal separator ({@link Locale#US}).
     * For example: {@code "47.400000,14.400000,48.700000,17.200000"}.
     *
     * @return A string representation of the bounding box for API usage.
     */
    public String toApiString() {
        // Using Locale.US ensures that '.' is used as the decimal separator,
        // which is common for APIs.
        return String.format(Locale.US, "%.6f,%.6f,%.6f,%.6f", minLat, minLon, maxLat, maxLon);
    }
}
