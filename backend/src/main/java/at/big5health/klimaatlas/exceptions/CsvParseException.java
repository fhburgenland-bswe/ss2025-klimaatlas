package at.big5health.klimaatlas.exceptions;

import java.util.List;

/**
 * Exception thrown when CSV parsing of population centers fails due to invalid data.
 * This includes format issues, missing columns, number parsing errors, or duplicate entries.
 * The collected error messages can be retrieved via {@link #getErrors()}.
 */
public class CsvParseException extends RuntimeException {

    private final List<String> errors;

    /**
     * Constructs a new CsvParseException with a list of detailed error messages.
     *
     * @param errors list of individual parsing errors (e.g. line numbers, descriptions)
     */
    public CsvParseException(List<String> errors) {
        super("CSV parsing failed with " + errors.size() + " errors.");
        this.errors = errors;
    }

    /**
     * Returns the list of parsing errors that caused this exception.
     *
     * @return list of error messages
     */
    public List<String> getErrors() {
        return errors;
    }
}