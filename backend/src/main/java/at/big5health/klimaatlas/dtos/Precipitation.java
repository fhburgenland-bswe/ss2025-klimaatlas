package at.big5health.klimaatlas.dtos;

import lombok.Getter;

@Getter
public enum Precipitation {
    RAIN("rain"),
    DRIZZLE("drizzle"),
    SNOW("snow"),
    SLEET("sleet"),
    HAIL("hail"),
    FREEZING_RAIN("freezing rain"),
    FREEZING_DRIZZLE("freezing drizzle"),
    ICE_PELLETS("ice pellets"),
    GRAUPEL( "graupel"),
    NONE("none")
    ;

    private final String message;

    Precipitation(String message) {
        this.message = message;
    }
}
