package at.big5health.klimaatlas.dtos.spartacus;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;
import java.util.List;

/**
 * Represents a specific weather parameter, including its data values, unit,
 * and descriptive name, as part of {@link SpartacusProperties}.
 * <p>
 * The {@code data} field typically contains a list of numerical values for the
 * parameter, though often it might just be a single value for daily summaries.
 * The {@link JsonIgnoreProperties @JsonIgnoreProperties(ignoreUnknown = true)}
 * annotation allows for flexible deserialization.
 * Lombok's {@link Data @Data} annotation generates standard boilerplate code.
 *
 * @see SpartacusProperties
 */
@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class SpartacusParameter {

    /**
     * A list of numerical data values for this parameter.
     * For daily summary parameters like temperature or precipitation, this list
     * often contains a single {@link Double} value.
     */
    private List<Double> data;

    /**
     * The unit of measurement for the data values (e.g., "°C", "mm").
     */
    private String unit;

    /**
     * A descriptive name for the weather parameter (e.g., "Maximum Temperature",
     * "Daily Precipitation Sum").
     */
    private String name;
}
