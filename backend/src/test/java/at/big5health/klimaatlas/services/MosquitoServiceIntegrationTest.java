package at.big5health.klimaatlas.services;

import okhttp3.mockwebserver.MockWebServer;
import at.big5health.klimaatlas.dtos.MosquitoOccurrenceDTO;
import okhttp3.mockwebserver.MockResponse;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.web.reactive.function.client.WebClient;
import java.io.IOException;
import java.util.List;


public class MosquitoServiceIntegrationTest {

    private MockWebServer mockWebServer;
    private MosquitoService mosquitoService;

    @BeforeEach
    public void setUp() throws IOException {
        mockWebServer = new MockWebServer();
        mockWebServer.start();

        String baseUrl = mockWebServer.url("/").toString();
        WebClient webClient = WebClient.builder().baseUrl(baseUrl).build();
        mosquitoService = new MosquitoService(webClient);
    }

    @AfterEach
    public void tearDown() throws IOException {
        mockWebServer.shutdown();
    }

    @Test
    public void testGetOccurrences_fromMockServer() {
        String jsonResponse = """
                    {
                      "results": [
                        {
                          "decimalLatitude": 47.062592,
                          "decimalLongitude": 15.448713,
                          "species": "Aedes albopictus",
                          "eventDate": "2025-02-27T15:55:05"
                        }
                      ]
                    }
                """;

        mockWebServer.enqueue(new MockResponse()
                .setBody(jsonResponse)
                .addHeader("Content-Type", "application/json"));

        List<MosquitoOccurrenceDTO> result = mosquitoService.getOccurrences();

        Assertions.assertFalse(result.isEmpty());
        MosquitoOccurrenceDTO dto = result.getLast();
        Assertions.assertEquals(47.062592, dto.getLatitude(), 0.0001);
        Assertions.assertEquals(15.448713, dto.getLongitude(), 0.0001);
        Assertions.assertEquals("Aedes albopictus", dto.getSpecies());
        Assertions.assertEquals("2025-02-27T15:55:05", dto.getEventDate());
    }

}
