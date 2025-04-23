package at.big5health.klimaatlas.dtos.spartacus; // Updated package

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;
import java.util.List;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class SpartacusGeometry {
    private List<Double> coordinates; // [longitude, latitude]
}