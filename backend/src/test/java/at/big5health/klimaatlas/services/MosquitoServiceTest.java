package at.big5health.klimaatlas.services;

import at.big5health.klimaatlas.dtos.MosquitoOccurrenceDTO;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.verify;


@ExtendWith(MockitoExtension.class)
public class MosquitoServiceTest {

    @Mock
    WebClient webClient;

    @Mock
    private WebClient.RequestHeadersUriSpec requestHeadersUriSpec;

    @Mock
    private WebClient.RequestHeadersSpec requestHeadersSpec;

    @Mock
    private WebClient.ResponseSpec responseSpec;

    MosquitoService mosquitoService;

    @BeforeEach
    void setup() {
        mosquitoService = new MosquitoService(webClient);
    }

    @Test
    void getOccurrences_shouldReturnMappedResults() {

        Map<String, Object> occurrence1 = new HashMap<>();
        occurrence1.put("decimalLatitude", 47.123);
        occurrence1.put("decimalLongitude", 15.456);
        occurrence1.put("species", "Aedes albopictus");
        occurrence1.put("eventDate", "2023-07-15");

        Map<String, Object> occurrence2 = new HashMap<>();
        occurrence2.put("decimalLatitude", 48.234);
        occurrence2.put("decimalLongitude", 16.789);
        occurrence2.put("species", "Culex pipiens");
        occurrence2.put("eventDate", "2023-08-20");

        Map<String, Object> response = new HashMap<>();
        response.put("results", List.of(occurrence1, occurrence2));

        String expectedUrl = "https://api.gbif.org/v1/occurrence/search?country=AT&taxon_key=3346&hasCoordinate=true&limit=1000&year=" + LocalDate.now().getYear();

        when(webClient.get()).thenReturn(requestHeadersUriSpec);
        when(requestHeadersUriSpec.uri(expectedUrl)).thenReturn(requestHeadersSpec);
        when(requestHeadersSpec.retrieve()).thenReturn(responseSpec);
        when(responseSpec.bodyToMono(Map.class)).thenReturn(Mono.just(response));

        List<MosquitoOccurrenceDTO> result = mosquitoService.getOccurrences();

        assertEquals(2, result.size());

        MosquitoOccurrenceDTO dto1 = result.getFirst();
        assertEquals(47.123, dto1.getLatitude());
        assertEquals(15.456, dto1.getLongitude());
        assertEquals("Aedes albopictus", dto1.getSpecies());
        assertEquals("2023-07-15", dto1.getEventDate());

        MosquitoOccurrenceDTO dto2 = result.get(1);
        assertEquals(48.234, dto2.getLatitude());
        assertEquals(16.789, dto2.getLongitude());
        assertEquals("Culex pipiens", dto2.getSpecies());
        assertEquals("2023-08-20", dto2.getEventDate());

        verify(webClient).get();
        verify(requestHeadersUriSpec).uri(expectedUrl);

    }

    @Test
    void getOccurrences_whenSpeciesOrEventDateMissing_shouldUseDefaultValues() {

        Map<String, Object> occurrence = new HashMap<>();
        occurrence.put("decimalLatitude", 47.123);
        occurrence.put("decimalLongitude", 15.456);

        Map<String, Object> response = new HashMap<>();
        response.put("results", List.of(occurrence));

        when(webClient.get()).thenReturn(requestHeadersUriSpec);
        when(requestHeadersUriSpec.uri(anyString())).thenReturn(requestHeadersSpec);
        when(requestHeadersSpec.retrieve()).thenReturn(responseSpec);
        when(responseSpec.bodyToMono(Map.class)).thenReturn(Mono.just(response));

        List<MosquitoOccurrenceDTO> result = mosquitoService.getOccurrences();

        assertEquals(1, result.size());

        MosquitoOccurrenceDTO dto = result.getFirst();
        assertEquals(47.123, dto.getLatitude());
        assertEquals(15.456, dto.getLongitude());
        assertEquals("Unknown", dto.getSpecies());
        assertEquals("Unknown", dto.getEventDate());
    }

    @Test
    void getOccurrences_whenResultsIsNull_shouldReturnEmptyList() {

        Map<String, Object> response = new HashMap<>();
        response.put("results", null);

        when(webClient.get()).thenReturn(requestHeadersUriSpec);
        when(requestHeadersUriSpec.uri(anyString())).thenReturn(requestHeadersSpec);
        when(requestHeadersSpec.retrieve()).thenReturn(responseSpec);
        when(responseSpec.bodyToMono(Map.class)).thenReturn(Mono.just(response));

        List<MosquitoOccurrenceDTO> result = mosquitoService.getOccurrences();

        Assertions.assertTrue(result.isEmpty());

    }

}
