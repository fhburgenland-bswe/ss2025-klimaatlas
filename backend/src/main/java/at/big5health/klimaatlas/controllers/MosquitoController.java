package at.big5health.klimaatlas.controllers;

import at.big5health.klimaatlas.dtos.MosquitoOccurrenceDTO;
import at.big5health.klimaatlas.services.MosquitoService;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.AllArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import java.util.List;

/**
 * REST controller that provides endpoints for accessing mosquito occurrence data.
 * <p>
 * This controller exposes a single GET endpoint that returns a list of mosquito occurrences
 * in Austria for the current year.
 */
@RestController
@RequestMapping("/mosquitoes")
@CrossOrigin("*")
@AllArgsConstructor
@Tag(name = "Mosquito occurrence", description = "API for accessing mosquito occurrence data")
public class MosquitoController {

    private final MosquitoService mosquitoService;

    /**
     * Handles HTTP GET requests to retrieve all mosquito occurrences.
     * <p>
     * This method delegates the data fetching to {@link MosquitoService} and returns the results
     * as a list of {@link MosquitoOccurrenceDTO} objects.
     *
     * @return a {@link ResponseEntity} containing the list of mosquito occurrences
     */
    @GetMapping
    @ApiResponse(responseCode = "200", description = "Success status")
    public ResponseEntity<List<MosquitoOccurrenceDTO>> getAllMosquitoOccurrences() {
        List<MosquitoOccurrenceDTO> data = mosquitoService.getOccurrences();
        return ResponseEntity.ok(data);
    }

}
