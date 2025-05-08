package at.big5health.klimaatlas.grid;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * Represents information about a single cell within a geographical grid.
 * <p>
 * This class holds a unique identifier for the cell, its geographical
 * bounding box, and the target coordinates (typically the center)
 * used for data retrieval or analysis within that cell.
 * Lombok's {@link Getter @Getter} and {@link AllArgsConstructor @AllArgsConstructor}
 * are used for boilerplate code generation.
 *
 * @see GridUtil
 * @see BoundingBox
 */
@Getter
@AllArgsConstructor
public class GridCellInfo {

    /**
     * A unique identifier for this grid cell.
     * Often generated based on its target coordinates.
     * Example: {@code "cell_48.208170_16.373820"}.
     */
    private String cellId;

    /**
     * The geographical bounding box that defines the spatial extent of this grid cell.
     *
     * @see BoundingBox
     */
    private BoundingBox bbox;

    /**
     * The target latitude for this grid cell, typically its center point,
     * in decimal degrees. This coordinate is often used for API calls
     * or data sampling.
     */
    private double targetLatitude;

    /**
     * The target longitude for this grid cell, typically its center point,
     * in decimal degrees. This coordinate is often used for API calls
     * or data sampling.
     */
    private double targetLongitude;
}
