package at.big5health.klimaatlas.exceptions;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(value = HttpStatus.NOT_FOUND)
public class WeatherDataNotFoundException extends RuntimeException {

    public WeatherDataNotFoundException(ErrorMessages errorMessages) {
        super(errorMessages.getMessage()); // Use the enum's message directly
    }

     //Constructor with formatting if needed for this type
     public WeatherDataNotFoundException(ErrorMessages errorMessages, Object... args) {
         super(errorMessages.format(args));
     }
}
