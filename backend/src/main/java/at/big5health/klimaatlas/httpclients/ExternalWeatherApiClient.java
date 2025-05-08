package at.big5health.klimaatlas.httpclients;

import at.big5health.klimaatlas.dtos.spartacus.SpartacusFeatureCollection;
import at.big5health.klimaatlas.exceptions.ErrorMessages;
import at.big5health.klimaatlas.exceptions.ExternalApiException;
import at.big5health.klimaatlas.grid.BoundingBox;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatusCode;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.ClientResponse;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.util.UriComponentsBuilder;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.function.Function;

/**
 * Client component for interacting with the external Spartacus weather API.
 * <p>
 * This client is responsible for fetching grid-based weather data, such as
 * temperature and precipitation, for specified geographical areas (bounding boxes)
 * and dates. It uses Spring's reactive {@link WebClient} for making HTTP requests.
 * Configuration details like the API base URL are injected from application properties.
 * <p>
 * Includes error handling for API responses and request timeouts, mapping
 * issues to {@link ExternalApiException}.
 *
 * @see WebClient
 * @see SpartacusFeatureCollection
 * @see BoundingBox
 * @see ExternalApiException
 * @author YourName or Klimaatlas Team
 * @since 1.0.0
 */
@Component
public class ExternalWeatherApiClient {

    private static final Logger LOG = LoggerFactory.getLogger(ExternalWeatherApiClient.class);
    private final WebClient webClient;
    private final String spartacusBaseUrl;

    /**
     * Holds the most recently constructed URI, primarily for logging purposes within error handlers.
     * This field is updated before each API call in {@link #fetchGridData(BoundingBox, LocalDate)}.
     */
    private String lastConstructedUri;

    /**
     * Constructs an {@code ExternalWeatherApiClient} with a configured {@link WebClient}.
     *
     * @param webClientBuilder The Spring {@link WebClient.Builder} used to construct the WebClient instance.
     * @param spartacusBaseUrl The base URL for the Spartacus API, injected from the
     *                         application property {@code spartacus.api.baseUrl}.
     */
    public ExternalWeatherApiClient(
            WebClient.Builder webClientBuilder,
            @Value("${spartacus.api.baseUrl}") String spartacusBaseUrl) {
        this.webClient = webClientBuilder.baseUrl(spartacusBaseUrl).build();
        this.spartacusBaseUrl = spartacusBaseUrl; // Also store for URI construction if needed elsewhere
    }

    /**
     * Fetches grid-based weather data from the Spartacus API for a given bounding box and date.
     * <p>
     * It requests parameters for maximum temperature ("TX"), minimum temperature ("TN"),
     * and precipitation ("RR"). The response is expected in GeoJSON format and is
     * mapped to a {@link SpartacusFeatureCollection}.
     * <p>
     * The method includes:
     * <ul>
     *   <li>Error handling for HTTP error statuses from the API.</li>
     *   <li>A request timeout of 15 seconds.</li>
     *   <li>Mapping of various errors (timeout, non-HTTP errors) to {@link ExternalApiException}.</li>
     * </ul>
     *
     * @param bbox The {@link BoundingBox} defining the geographical area of interest.
     * @param date The {@link LocalDate} for which to fetch the weather data.
     * @return A {@link Mono} emitting a {@link SpartacusFeatureCollection} upon successful
     *         retrieval and parsing. The Mono will emit an error (typically
     *         {@link ExternalApiException}) if the API call fails, times out,
     *         or an unexpected error occurs.
     * @throws ExternalApiException if the API returns an error, the request times out,
     *                              or an unexpected issue occurs during the reactive flow.
     */
    public Mono<SpartacusFeatureCollection> fetchGridData(BoundingBox bbox, LocalDate date) {
        String dateString = date.format(DateTimeFormatter.ISO_DATE);
        String parameters = "TX,TN,RR"; // Max/Min Temp, Precipitation

        // Store URI in the field for access in the handler
        this.lastConstructedUri = UriComponentsBuilder.fromHttpUrl(spartacusBaseUrl)
                .queryParam("start", dateString)
                .queryParam("end", dateString)
                .queryParam("bbox", bbox.toApiString())
                .queryParam("parameters", parameters)
                .queryParam("response_format", "geojson")
                .toUriString();

        LOG.debug("Calling Spartacus API: {}", lastConstructedUri);

        // Define the error handling function
        Function<ClientResponse, Mono<? extends Throwable>> errorHandler = clientResponse ->
                clientResponse.bodyToMono(String.class)
                        .defaultIfEmpty("[Empty or Unreadable Error Body from API]")
                        .flatMap(errorBody -> {
                            String errorMsg = String.format("Spartacus API Error %s for URI %s: %s",
                                    clientResponse.statusCode(), this.lastConstructedUri, errorBody);
                            LOG.error(errorMsg);
                            // Ensure the ExternalApiException uses a message from ErrorMessages
                            return Mono.error(new ExternalApiException(ErrorMessages.EXTERNAL_API_FAILURE));
                        });

        return this.webClient.get()
                .uri(this.lastConstructedUri)
                .retrieve()
                .onStatus(HttpStatusCode::isError, errorHandler)
                .bodyToMono(SpartacusFeatureCollection.class)
                .timeout(Duration.ofSeconds(15), Mono.error(new ExternalApiException(ErrorMessages.EXTERNAL_API_TIMEOUT)))
                .doOnError(e -> !(e instanceof ExternalApiException), ex -> // Log non-ExternalApiExceptions that might occur before onErrorMap
                        LOG.error("Unexpected error during WebClient call to {}: {}", this.lastConstructedUri, ex.getMessage(), ex))
                .onErrorMap(e -> !(e instanceof ExternalApiException), // Map other exceptions to ExternalApiException
                        e -> new ExternalApiException(ErrorMessages.EXTERNAL_API_FAILURE, e));
    }
}
