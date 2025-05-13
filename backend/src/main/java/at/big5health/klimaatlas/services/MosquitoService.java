package at.big5health.klimaatlas.services;

import at.big5health.klimaatlas.dtos.MosquitoOccurrenceDTO;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import java.time.LocalDate;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Service class responsible for retrieving mosquito occurrence data from the GBIF API.
 * <p>
 * This service fetches data for Austria (country code "AT") and for a specific taxon (Aedes genus, taxon key 3346),
 * returning a list of mosquito occurrence records for the current year.
 */
@Service
public class MosquitoService {

    private final WebClient webClient;

    /**
     * Constructs a new {@code MosquitoService} with the given {@link WebClient}.
     *
     * @param webClient the WebClient used to perform HTTP requests
     */
    public MosquitoService(WebClient webClient) {
        this.webClient = webClient;
    }

    /**
     * Retrieves a list of mosquito occurrences in Austria for the current year from the GBIF API.
     * <p>
     * The request filters by:
     * <ul>
     *     <li>Country: Austria (AT)</li>
     *     <li>Taxon Key: 3346 (representing Aedes mosquitoes)</li>
     *     <li>Presence of coordinates</li>
     *     <li>Current year</li>
     *     <li>Maximum of 1000 records</li>
     * </ul>
     * <p>
     * If no results are found or if the API call fails, an empty list is returned.
     *
     * @return a list of {@link MosquitoOccurrenceDTO} representing the found mosquito occurrences
     */
    public List<MosquitoOccurrenceDTO> getOccurrences() {

        String url = "https://api.gbif.org/v1/occurrence/search?country=AT&taxon_key=3346&hasCoordinate=true&limit=1000&year=" + LocalDate.now().getYear();

        Map response = webClient.get()
                .uri(url)
                .retrieve()
                .bodyToMono(Map.class)
                .block();

        List<Map<String, Object>> results = (List<Map<String, Object>>) response.get("results");
        if (results == null) return Collections.emptyList();

        return results.stream()
                .map(entry -> new MosquitoOccurrenceDTO(
                        (Double) entry.get("decimalLatitude"),
                        (Double) entry.get("decimalLongitude"),
                        (String) entry.getOrDefault("species", "Unknown"),
                        (String) entry.getOrDefault("eventDate", "Unknown")
                ))
                .collect(Collectors.toList());
    }

}
