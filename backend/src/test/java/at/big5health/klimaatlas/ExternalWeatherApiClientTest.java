package at.big5health.klimaatlas;

import at.big5health.klimaatlas.dtos.spartacus.SpartacusFeatureCollection;
import at.big5health.klimaatlas.exceptions.ErrorMessages;
import at.big5health.klimaatlas.exceptions.ExternalApiException;
import at.big5health.klimaatlas.grid.BoundingBox;
import at.big5health.klimaatlas.httpclients.ExternalWeatherApiClient;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import okhttp3.mockwebserver.RecordedRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.AfterAll;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.io.IOException;
import java.time.Duration;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class ExternalWeatherApiClientTest {

    private MockWebServer mockWebServer;
    private ExternalWeatherApiClient apiClient;
    private ObjectMapper objectMapper = new ObjectMapper(); // For creating JSON bodies if needed

    private BoundingBox testBbox;
    private LocalDate testDate;

    @BeforeAll
    void setUpServer() throws IOException {
        mockWebServer = new MockWebServer();
        mockWebServer.start();
    }

    @AfterAll
    void tearDownServer() throws IOException {
        mockWebServer.shutdown();
    }

    @BeforeEach
    void setUp() {
        // Point the WebClient (and thus the apiClient) to the mock server for each test
        String baseUrl = mockWebServer.url("/").toString(); // Get mock server URL
        // Pass the base URL directly, overriding the application.properties value for the test
        apiClient = new ExternalWeatherApiClient(WebClient.builder(), baseUrl);

        // Initialize common test data
        testDate = LocalDate.of(2025, 4, 21);
        testBbox = new BoundingBox(48.1, 16.1, 48.2, 16.2);
    }

    @Test
    void fetchGridData_whenApiReturns200Ok_shouldReturnData() throws InterruptedException, JsonProcessingException {
        // Arrange
        // Create a realistic (but minimal) successful JSON response body
        String successJson = """
                {
                  "type": "FeatureCollection",
                  "features": [
                    {
                      "type": "Feature",
                      "geometry": {"type": "Point", "coordinates": [16.15, 48.15]},
                      "properties": {
                        "parameters": {
                          "TX": {"data": [25.5]},
                          "TN": {"data": [10.1]},
                          "RR": {"data": [0.0]}
                        }
                      }
                    }
                  ]
                }
                """;

        mockWebServer.enqueue(new MockResponse()
                .setResponseCode(200)
                .setHeader("Content-Type", "application/json")
                .setBody(successJson));

        // Act
        Mono<SpartacusFeatureCollection> resultMono = apiClient.fetchGridData(testBbox, testDate);

        // Assert using StepVerifier for reactive streams
        StepVerifier.create(resultMono)
                .assertNext(collection -> {
                    assertThat(collection).isNotNull();
                    assertThat(collection.getFeatures()).isNotNull().hasSize(1);
                    assertThat(collection.getFeatures().get(0).getProperties().getParameters())
                            .containsKey("TX")
                            .containsKey("TN")
                            .containsKey("RR");
                    assertThat(collection.getFeatures().get(0).getProperties().getParameters().get("TX").getData().get(0)).isEqualTo(25.5);
                })
                .verifyComplete(); // Verify the Mono completes successfully

        // Verify the request sent to the mock server
        RecordedRequest recordedRequest = mockWebServer.takeRequest();
        assertThat(recordedRequest.getMethod()).isEqualTo("GET");
        String expectedDateStr = testDate.format(DateTimeFormatter.ISO_DATE);
        String expectedBboxStr = testBbox.toApiString();
        assertThat(recordedRequest.getPath()).contains(
                "start=" + expectedDateStr,
                "end=" + expectedDateStr,
                "bbox=" + expectedBboxStr,
                "parameters=TX,TN,RR",
                "response_format=geojson"
        );
    }

    @Test
    void fetchGridData_whenApiReturns404NotFound_shouldReturnApiError() throws InterruptedException {
        // Arrange
        mockWebServer.enqueue(new MockResponse()
                .setResponseCode(404)
                .setHeader("Content-Type", "application/json")
                .setBody("{\"error\":\"Data not found\"}")); // Example error body

        // Act
        Mono<SpartacusFeatureCollection> resultMono = apiClient.fetchGridData(testBbox, testDate);

        // Assert
        StepVerifier.create(resultMono)
                .expectErrorMatches(throwable ->
                        throwable instanceof ExternalApiException &&
                                throwable.getMessage().equals(ErrorMessages.EXTERNAL_API_FAILURE.getMessage())
                )
                .verify();

        // Verify the request was still made
        RecordedRequest recordedRequest = mockWebServer.takeRequest();
        assertThat(recordedRequest.getMethod()).isEqualTo("GET");
    }

    @Test
    void fetchGridData_whenApiReturns500ServerError_shouldReturnApiError() throws InterruptedException {
        // Arrange
        mockWebServer.enqueue(new MockResponse()
                .setResponseCode(500)
                .setHeader("Content-Type", "application/json")
                .setBody("{\"error\":\"Server exploded\"}"));

        // Act
        Mono<SpartacusFeatureCollection> resultMono = apiClient.fetchGridData(testBbox, testDate);

        // Assert
        StepVerifier.create(resultMono)
                .expectErrorMatches(throwable ->
                        throwable instanceof ExternalApiException &&
                                throwable.getMessage().equals(ErrorMessages.EXTERNAL_API_FAILURE.getMessage())
                )
                .verify();

        RecordedRequest recordedRequest = mockWebServer.takeRequest();
        assertThat(recordedRequest.getMethod()).isEqualTo("GET");
    }

    @Test
    void fetchGridData_whenTestTimeoutOccurs_shouldReturnTimeoutException() throws InterruptedException { // Renamed for clarity
        // Arrange
        // Enqueue a response with a delay longer than the *test's* timeout
        mockWebServer.enqueue(new MockResponse()
                .setBodyDelay(100, TimeUnit.MILLISECONDS) // Delay > 10ms test timeout
                .setResponseCode(200)
                .setBody("{}"));

        // Act
        // Call the actual client method and apply a *short test-specific timeout*
        Mono<SpartacusFeatureCollection> resultMonoWithTestTimeout = apiClient.fetchGridData(testBbox, testDate)
                .timeout(Duration.ofMillis(10)); // Test timeout applied *after* fetchGridData returns its Mono

        // Assert
        StepVerifier.create(resultMonoWithTestTimeout)
                // Expect the raw TimeoutException from the test's .timeout() operator
                .expectError(java.util.concurrent.TimeoutException.class)
                .verify(); // Verify the error occurred

        // Verify the request was still made to the mock server
        RecordedRequest recordedRequest = mockWebServer.takeRequest(1, TimeUnit.SECONDS);
        assertThat(recordedRequest).isNotNull();
        assertThat(recordedRequest.getMethod()).isEqualTo("GET");
    }
}
