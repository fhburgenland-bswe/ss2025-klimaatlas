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

@Component
public class ExternalWeatherApiClient {

    private static final Logger LOG = LoggerFactory.getLogger(ExternalWeatherApiClient.class);
    private final WebClient webClient;
    private final String spartacusBaseUrl;
    private String uri; // Added field to hold URI for logging in handler

    public ExternalWeatherApiClient(
            WebClient.Builder webClientBuilder,
            @Value("${spartacus.api.baseUrl}") String spartacusBaseUrl) {
        this.webClient = webClientBuilder.baseUrl(spartacusBaseUrl).build();
        this.spartacusBaseUrl = spartacusBaseUrl;
    }

    public Mono<SpartacusFeatureCollection> fetchGridData(BoundingBox bbox, LocalDate date) {
        String dateString = date.format(DateTimeFormatter.ISO_DATE);
        String parameters = "TX,TN,RR";

        // Store URI in the field for access in the handler
        this.uri = UriComponentsBuilder.fromHttpUrl(spartacusBaseUrl)
                .queryParam("start", dateString)
                .queryParam("end", dateString)
                .queryParam("bbox", bbox.toApiString())
                .queryParam("parameters", parameters)
                .queryParam("response_format", "geojson")
                .toUriString();

        LOG.debug("Calling Spartacus API: {}", uri);

        // Define the error handling function using defaultIfEmpty
        Function<ClientResponse, Mono<? extends Throwable>> errorHandler = clientResponse ->
                clientResponse.bodyToMono(String.class)
                        .defaultIfEmpty("[Empty or Unreadable Error Body]") // Provide default if body empty/unreadable
                        .flatMap(errorBody -> { // flatMap always receives a String now
                            String errorMsg = String.format("Spartacus API Error %s for URI %s: %s",
                                    clientResponse.statusCode(), this.uri, errorBody); // Use field uri
                            LOG.error(errorMsg);
                            // Always return Mono.error here
                            return Mono.error(new ExternalApiException(ErrorMessages.EXTERNAL_API_FAILURE));
                        });

        return this.webClient.get()
                .uri(this.uri) // Use field uri
                .retrieve()
                .onStatus(HttpStatusCode::isError, errorHandler) // Use the simplified handler
                .bodyToMono(SpartacusFeatureCollection.class)
                .timeout(Duration.ofSeconds(15), Mono.error(new ExternalApiException(ErrorMessages.EXTERNAL_API_TIMEOUT)))
                .doOnError(e -> !(e instanceof ExternalApiException), ex ->
                        LOG.error("Error during WebClient call to {}: {}", this.uri, ex.getMessage(), ex)) // Use field uri
                .onErrorMap(e -> !(e instanceof ExternalApiException),
                        e -> new ExternalApiException(ErrorMessages.EXTERNAL_API_FAILURE, e));
    }
}
