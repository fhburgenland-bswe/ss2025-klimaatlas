package at.big5health.klimaatlas.dtos.spartacus;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;
import java.util.List;

/**
 * Represents a collection of features, typically corresponding to a GeoJSON
 * FeatureCollection structure, as returned by the Spartacus API.
 * <p>
 * This DTO serves as the top-level container for weather data points (features)
 * retrieved from the external service. The {@link JsonIgnoreProperties @JsonIgnoreProperties(ignoreUnknown = true)}
 * annotation allows for flexible deserialization if the API adds new, unmapped fields.
 * Lombok's {@link Data @Data} annotation generates standard boilerplate code.
 *
 * @see SpartacusFeature
 * @see at.big5health.klimaatlas.httpclients.ExternalWeatherApiClient
 */
@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class SpartacusFeatureCollection {

    /**
     * A list of {@link SpartacusFeature} objects, where each feature typically
     * represents weather data for a specific point or area.
     */
    private List<SpartacusFeature> features;
}
