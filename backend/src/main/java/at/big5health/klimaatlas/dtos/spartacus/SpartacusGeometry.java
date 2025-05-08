package at.big5health.klimaatlas.dtos.spartacus;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;
import java.util.List;

/**
 * Represents the geometric information for a {@link SpartacusFeature},
 * typically containing coordinates.
 * <p>
 * In the context of the Spartacus API, this usually holds a list of two doubles
 * representing [longitude, latitude].
 * The {@link JsonIgnoreProperties @JsonIgnoreProperties(ignoreUnknown = true)}
 * annotation allows for flexible deserialization.
 * Lombok's {@link Data @Data} annotation generates standard boilerplate code.
 *
 * @see SpartacusFeature
 */
@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class SpartacusGeometry {

    /**
     * A list of coordinates defining the geometry.
     * For point data from the Spartacus API, this is expected to be a list
     * containing two {@link Double} values: {@code [longitude, latitude]}.
     * Index 0: Longitude
     * Index 1: Latitude
     */
    private List<Double> coordinates;
}
