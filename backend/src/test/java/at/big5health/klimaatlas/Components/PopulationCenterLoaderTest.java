package at.big5health.klimaatlas.Components;

import at.big5health.klimaatlas.config.AustrianPopulationCenter;
import at.big5health.klimaatlas.exceptions.CsvParseException;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.AssertionsForClassTypes.catchThrowable;

class PopulationCenterLoaderTest {

    private PopulationCenterLoader prepareLoaderWithPath(String path) {
        PopulationCenterLoader loader = new PopulationCenterLoader();
        try {
            Field field = PopulationCenterLoader.class.getDeclaredField("csvFilePath");
            field.setAccessible(true);
            field.set(loader, path);
        } catch (Exception e) {
            throw new RuntimeException("Failed to set test CSV path", e);
        }
        return loader;
    }

    @Test
    void loadFromCSV_shouldLoadValidCenters() {
        String path = "src/test/resources/valid.csv";
        PopulationCenterLoader loader = prepareLoaderWithPath(path);

        List<AustrianPopulationCenter> result = loader.loadFromCSV();

        assertThat(result).hasSize(2);
        assertThat(result.getFirst().getDisplayName()).isEqualTo("Wien");
    }

    @Test
    void loadFromCSV_shouldThrowOnInvalidNumber() {
        String path = "src/test/resources/invalid_format.csv";
        PopulationCenterLoader loader = prepareLoaderWithPath(path);

        Throwable thrown = catchThrowable(loader::loadFromCSV);

        assertThat(thrown)
                .isInstanceOf(CsvParseException.class)
                .satisfies(t -> {
                    CsvParseException ex = (CsvParseException) t;
                    assertThat(ex.getErrors())
                            .anyMatch(err -> err.contains("Invalid number format"));
                });
    }

    @Test
    void loadFromCSV_shouldThrowOnDuplicates() {
        String path = "src/test/resources/duplicate.csv";
        PopulationCenterLoader loader = prepareLoaderWithPath(path);

        Throwable thrown = catchThrowable(loader::loadFromCSV);

        assertThat(thrown)
                .isInstanceOf(CsvParseException.class)
                .satisfies(t -> {
                    CsvParseException ex = (CsvParseException) t;
                    assertThat(ex.getErrors())
                            .anyMatch(error -> error.contains("Duplicate at row"));
                });
    }

    @Test
    void loadFromCSV_shouldThrowOnMissingColumns() {
        String path = "src/test/resources/missing.csv";
        PopulationCenterLoader loader = prepareLoaderWithPath(path);

        Throwable thrown = catchThrowable(loader::loadFromCSV);

        assertThat(thrown)
                .isInstanceOf(CsvParseException.class)
                .satisfies(t -> {
                    CsvParseException ex = (CsvParseException) t;
                    assertThat(ex.getErrors())
                            .anyMatch(error -> error.contains("Not enough columns"));
                });
    }

    @Test
    void loadFromCSV_shouldThrowIfFileDoesNotExist() {
        String path = "src/test/resources/nonexistent.csv";
        PopulationCenterLoader loader = prepareLoaderWithPath(path);

        assertThatThrownBy(loader::loadFromCSV)
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("CSV file not found");
    }
}