package at.big5health.klimaatlas;

import at.big5health.klimaatlas.dtos.WeatherReportDTO;
import at.big5health.klimaatlas.grid.BoundingBox;
import at.big5health.klimaatlas.grid.GridCellInfo;
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
        // Mock WeatherService
        WeatherService weatherService = mock(WeatherService.class);
        GridUtil gridUtil = new GridUtil(); // Optional: auch mockbar, falls nötig

        // Simuliere: erster Aufruf schlägt fehl, zweiter liefert gültige Daten
        when(weatherService.getWeather(any(), anyDouble(), anyDouble(), any(LocalDate.class)))
                .thenThrow(new RuntimeException("Fehler bei erster Zelle"))
                .thenAnswer(invocation -> {
                    WeatherReportDTO dto = new WeatherReportDTO();
                    dto.setMinTemp(10.0);
                    dto.setMaxTemp(20.0);
                    return dto;
                });

        GridCacheService service = new GridCacheService() {{
            this.weatherService = weatherService;
            this.gridUtil = new GridUtil();
        }};

        Map<String, BoundingBox> testStates = new HashMap<>();
        testStates.put("Wien", new BoundingBox(48.1, 48.2, 16.2, 16.3));

        try {
            Field field = GridCacheService.class.getDeclaredField("austrianStates");
            field.setAccessible(true);
            field.set(service, testStates);
        } catch (Exception e) {
            fail("Konnte austrianStates nicht setzen: " + e.getMessage());
        }

        // When
        List<GridCacheService.GridTemperature> result = service.getTemperatureGridForState("Wien");

        // Then
        assertThat(result).isNotNull();
        assertThat(result.size()).isGreaterThanOrEqualTo(1);
        assertThat(result.get(0).temperature()).isEqualTo(15.0);
    }


}
