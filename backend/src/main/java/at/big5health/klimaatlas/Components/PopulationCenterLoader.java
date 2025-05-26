package at.big5health.klimaatlas.Components;

import at.big5health.klimaatlas.config.AustrianPopulationCenter;
import at.big5health.klimaatlas.exceptions.CsvParseException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.*;
import java.util.*;

/**
 * Loads Austrian population centers from a CSV file defined by configuration.
 * Validates structure, checks for duplicates, and parses numeric values.
 * If the file contains invalid data or duplicates, a {@link CsvParseException} is thrown.
 * The CSV path must be configured via the {@code population.centers.csv-path} property.
 */
@Component
public class PopulationCenterLoader {

    @Value("${population.centers.csv-path}")
    private String csvFilePath;

    /**
     * Reads and parses all population centers from the configured CSV file.
     * Skips the header, validates row format and numerical values.
     *
     * @return list of valid {@link AustrianPopulationCenter} entries
     * @throws CsvParseException if rows are malformed, invalid, or contain duplicates
     * @throws RuntimeException if file cannot be read
     */
    public List<AustrianPopulationCenter> loadFromCSV() {
        File file = new File(csvFilePath);
        if (!file.exists()) {
            throw new RuntimeException("CSV file not found at path: " + csvFilePath);
        }

        List<AustrianPopulationCenter> centers = new ArrayList<>();
        Set<String> uniqueKeys = new HashSet<>();
        List<String> duplicates = new ArrayList<>();
        List<String> errors = new ArrayList<>();

        try (BufferedReader reader = new BufferedReader(new FileReader(file))) {
            List<String> lines = reader.lines().skip(1).toList(); // skip header

            for (int i = 0; i < lines.size(); i++) {
                String line = lines.get(i);
                String[] parts = line.split(",");

                if (parts.length < 7) {
                    errors.add("Invalid line at row " + (i + 2) + ": Not enough columns");
                    continue;
                }

                try {
                    String displayName = parts[0];
                    double repLat = Double.parseDouble(parts[1]);
                    double repLon = Double.parseDouble(parts[2]);
                    double minLat = Double.parseDouble(parts[3]);
                    double minLon = Double.parseDouble(parts[4]);
                    double maxLat = Double.parseDouble(parts[5]);
                    double maxLon = Double.parseDouble(parts[6]);

                    String key = displayName.trim().toLowerCase() + "_" + repLat + "_" + repLon;
                    if (!uniqueKeys.add(key)) {
                        duplicates.add("Duplicate at row " + (i + 2) + ": " + key);
                        continue;
                    }

                    centers.add(new AustrianPopulationCenter(
                            displayName, repLat, repLon, minLat, minLon, maxLat, maxLon));

                } catch (NumberFormatException e) {
                    errors.add("Invalid number format at row " + (i + 2) + ": " + e.getMessage());
                }
            }

            if (!duplicates.isEmpty()) {
                System.err.println("[CSV WARNING] Duplicate entries detected:");
                errors.addAll(duplicates);
            }

            if (!errors.isEmpty()) {
                throw new CsvParseException(errors);
            }

        } catch (IOException e) {
            throw new RuntimeException("Failed to load population center data from: " + csvFilePath, e);
        }

        return centers;
    }
}