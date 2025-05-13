package at.big5health.klimaatlas.dtos;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Data Transfer Object (DTO) representing a single mosquito occurrence record.
 * <p>
 * This class contains basic information about a mosquito sighting, including its geographic
 * coordinates, species name, and the date the event was recorded.
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class MosquitoOccurrenceDTO {

    /**
     * The latitude coordinate of the mosquito occurrence.
     */
    private double latitude;

    /**
     * The longitude coordinate of the mosquito occurrence.
     */
    private double longitude;

    /**
     * The species name of the mosquito. If the species is not available, it may be set to "Unknown".
     */

    private String species;

    /**
     * The date the mosquito occurrence was recorded, typically in ISO 8601 format (e.g., "2024-05-13").
     * If not available, it may be set to "Unknown".
     */
    private String eventDate;

}
