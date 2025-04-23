package at.big5health.klimaatlas.dtos.spartacus; // Updated package

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;
import java.util.Map;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class SpartacusProperties {
    private Map<String, SpartacusParameter> parameters;
}