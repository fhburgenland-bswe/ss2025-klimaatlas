package at.big5health.klimaatlas.dtos.spartacus;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;
import java.util.Map;

/**
 * Represents the properties of a {@link SpartacusFeature}, containing a map
 * of weather parameters.
 * <p>
 * The keys of the map are typically short codes representing specific weather
 * parameters (e.g., "TX" for maximum temperature, "TN" for minimum temperature,
 * "RR" for precipitation). The values are {@link SpartacusParameter} objects
 * containing the data, unit, and name for that parameter.
 * The {@link JsonIgnoreProperties @JsonIgnoreProperties(ignoreUnknown = true)}
 * annotation allows for flexible deserialization.
 * Lombok's {@link Data @Data} annotation generates standard boilerplate code.
 *
 * @see SpartacusFeature
 * @see SpartacusParameter
 */
@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class SpartacusProperties {

    /**
     * A map where keys are weather parameter codes (e.g., "TX", "TN", "RR")
     * and values are {@link SpartacusParameter} objects holding the data
     * for that specific parameter.
     */
    private Map<String, SpartacusParameter> parameters;
}
