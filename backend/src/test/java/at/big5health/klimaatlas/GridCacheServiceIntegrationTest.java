package at.big5health.klimaatlas;

import at.big5health.klimaatlas.dtos.WeatherReportDTO;
import at.big5health.klimaatlas.grid.BoundingBox;
import at.big5health.klimaatlas.grid.GridCellInfo;
import at.big5health.klimaatlas.grid.GridTemperature;
import at.big5health.klimaatlas.grid.GridUtil;
import at.big5health.klimaatlas.services.GridCacheService;
import at.big5health.klimaatlas.services.WeatherService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import java.lang.reflect.Field;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Fail.fail;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyDouble;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@SpringBootTest
public class GridCacheServiceIntegrationTest {

    @Autowired
    private GridCacheService gridCacheService;

    @MockitoBean
    private WeatherService weatherService;


    @Test
    void testTemperatureGridForUnknownState_throwsException() {
        // Then
        org.junit.jupiter.api.Assertions.assertThrows(IllegalArgumentException.class, () -> {
            gridCacheService.getTemperatureGridForState("Atlantis");
        });
    }

    @Test
    void testGenerateGrid_returnsNonEmptyGrid() {

        BoundingBox bbox = new BoundingBox(48.0, 16.0, 48.3, 16.3);

        double resolution = 0.01;

        GridUtil gridUtil = new GridUtil();

        List<GridCellInfo> gridCells = gridUtil.generateGrid(bbox, resolution);

        gridCells.forEach(cell -> System.out.println(cell.getCellId()));

        assertThat(gridCells.size()).isGreaterThanOrEqualTo(1);

    }

    @Test
    void testWeatherServiceError_doesNotBreakGridCollection() {

        // Given: First call throws, second succeeds
        when(weatherService.getWeather(any(), anyDouble(), anyDouble(), any(LocalDate.class)))
                .thenThrow(new RuntimeException("Failed"))
                .thenReturn(new WeatherReportDTO() {{
                    setMinTemp(10.0);
                    setMaxTemp(20.0);
                }});

        // When
        List<GridTemperature> result = gridCacheService.getTemperatureGridForState("Wien");

        // Then
        assertThat(result.size()).isGreaterThanOrEqualTo(0);

    }



}
