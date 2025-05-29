package at.big5health.klimaatlas.services;

import at.big5health.klimaatlas.Components.PopulationCenterLoader;
import at.big5health.klimaatlas.config.AustrianPopulationCenter;
import at.big5health.klimaatlas.exceptions.CsvParseException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.List;
import java.util.function.Consumer;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.AssertionsForClassTypes.catchThrowable;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

public class PopulationCenterServiceTest {

    private PopulationCenterLoader loader;
    private PopulationCenterService service;

    @BeforeEach
    void setUp() {
        loader = mock(PopulationCenterLoader.class);
        service = new PopulationCenterService(loader);
    }

    @Test
    void init_shouldLoadCentersSuccessfully() {
        List<AustrianPopulationCenter> mockList = List.of(
                new AustrianPopulationCenter("Wien", 48.2, 16.3, 48.1, 16.1, 48.3, 16.5)
        );
        given(loader.loadFromCSV()).willReturn(mockList);

        service.init();

        assertThat(service.getAllCenters()).hasSize(1);
        assertThat(service.getAllCenters().getFirst().getDisplayName()).isEqualTo("Wien");
    }

    @Test
    void init_shouldHandleExceptionGracefully() {
        given(loader.loadFromCSV()).willThrow(new RuntimeException("Failed to load"));

        service.init();

        assertThat(service.getAllCenters()).isEmpty();
    }

    @Test
    void getAllCenters_shouldThrowIfLastCsvHadErrors() {
        service = new PopulationCenterService(loader);
        List<String> simulatedErrors = List.of("invalid row");

        setPrivateField(service, "lastCsvErrors", simulatedErrors);

        Throwable thrown = catchThrowable(service::getAllCenters);

        assertThat(thrown)
                .isInstanceOf(CsvParseException.class);

        CsvParseException ex = (CsvParseException) thrown;
        assertThat(ex.getErrors()).contains("invalid row");
    }

    @Test
    void refreshAndReCache_shouldReloadAndReCacheNewEntries() {
        AustrianPopulationCenter oldCenter = new AustrianPopulationCenter("Wien", 48.2, 16.3, 48.1, 16.1, 48.3, 16.5);
        AustrianPopulationCenter newCenter = new AustrianPopulationCenter("Graz", 47.1, 15.4, 47.0, 15.3, 47.2, 15.5);

        service = new PopulationCenterService(loader);
        setPrivateField(service, "centers", List.of(oldCenter));

        given(loader.loadFromCSV()).willReturn(List.of(oldCenter, newCenter));

        Consumer<AustrianPopulationCenter> mockReCache = mock(Consumer.class);
        service.refreshAndReCache(LocalDate.now(), mockReCache);

        verify(mockReCache).accept(newCenter);
        assertThat(service.getAllCenters()).hasSize(2);
    }

    @Test
    void refreshAndReCache_whenCsvParseException_shouldStoreErrors() {
        List<String> errorList = List.of("err1", "err2");
        CsvParseException ex = new CsvParseException(errorList);
        given(loader.loadFromCSV()).willThrow(ex);

        Consumer<AustrianPopulationCenter> mockReCache = mock(Consumer.class);

        Throwable thrown = catchThrowable(() ->
                service.refreshAndReCache(LocalDate.now(), mockReCache)
        );

        assertThat(thrown)
                .isInstanceOf(CsvParseException.class);

        assertThat(((CsvParseException) thrown).getErrors())
                .containsExactlyElementsOf(errorList);
    }

    @Test
    void refreshAndReCache_whenRuntimeException_shouldStoreMessage() {
        given(loader.loadFromCSV()).willThrow(new RuntimeException("unexpected"));

        Consumer<AustrianPopulationCenter> mockReCache = mock(Consumer.class);

        assertThatThrownBy(() -> service.refreshAndReCache(LocalDate.now(), mockReCache))
                .isInstanceOf(RuntimeException.class)
                .hasMessage("unexpected");
    }

    // --- Helper to set any private field via reflection ---
    private <T> void setPrivateField(Object target, String fieldName, T value) {
        try {
            var field = target.getClass().getDeclaredField(fieldName);
            field.setAccessible(true);
            field.set(target, value);
        } catch (Exception e) {
            throw new RuntimeException("Failed to set private field", e);
        }
    }
}