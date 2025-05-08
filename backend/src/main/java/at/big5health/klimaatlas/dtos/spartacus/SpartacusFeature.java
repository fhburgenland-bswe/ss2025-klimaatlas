package at.big5health.klimaatlas.dtos.spartacus;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;

/**
 * Represents a single feature within a {@link SpartacusFeatureCollection},
 * typically corresponding to a GeoJSON Feature structure.
 * <p>
 * Each feature contains geometric information ({@link SpartacusGeometry}) defining
 * its location and properties ({@link SpartacusProperties}) holding the actual
 * weather parameter data.
 * The {@link JsonIgnoreProperties @JsonIgnoreProperties(ignoreUnknown = true)}
 * annotation allows for flexible deserialization.
 * Lombok's {@link Data @Data} annotation generates standard boilerplate code.
 *
 * @see SpartacusGeometry
 * @see SpartacusProperties
 * @see SpartacusFeatureCollection
 */
@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class SpartacusFeature {

    /**
     * The geometric information for this feature, typically containing coordinates.
     *
     * @see SpartacusGeometry
     */
    private SpartacusGeometry geometry;

    /**
     * The properties associated with this feature, containing the weather parameters
     * and their values.
     *
     * @see SpartacusProperties
     */
    private SpartacusProperties properties;
}
