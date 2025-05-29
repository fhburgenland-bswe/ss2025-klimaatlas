package at.big5health.klimaatlas.grid;

/**
 * Represents a temperature reading at a specific geographical grid point.
 * <p>
 * This record is a simple, immutable data carrier for associating a temperature
 * value with its corresponding latitude and longitude.
 *
 * @param latitude    The geographical latitude of the grid point, in decimal degrees.
 * @param longitude   The geographical longitude of the grid point, in decimal degrees.
 * @param temperature The temperature at this grid point, typically in degrees Celsius.
 */
public record GridTemperature(double latitude, double longitude, double temperature) {
    // No additional methods or fields needed for a simple data carrier.
    // Getters (latitude(), longitude(), temperature()), equals(), hashCode(),
    // toString(), and a canonical constructor are automatically provided.
}
