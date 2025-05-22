import { translatePrecipitation } from "./precipitation-translator";

describe('translatePrecipitation', () => {
  it('should translate known precipitation types to German', () => {
    expect(translatePrecipitation('RAIN')).toBe('Regen');
    expect(translatePrecipitation('DRIZZLE')).toBe('Nieselregen');
    expect(translatePrecipitation('SNOW')).toBe('Schnee');
    expect(translatePrecipitation('SLEET')).toBe('Schneeregen');
    expect(translatePrecipitation('HAIL')).toBe('Hagel');
    expect(translatePrecipitation('FREEZING_RAIN')).toBe('gefrierender Regen');
    expect(translatePrecipitation('FREEZING_DRIZZLE')).toBe('gefrierender Nieselregen');
    expect(translatePrecipitation('ICE_PELLETS')).toBe('Eiskörner');
    expect(translatePrecipitation('GRAUPEL')).toBe('Graupel');
    expect(translatePrecipitation('NONE')).toBe('Kein Niederschlag');
  });

  it('should return "Kein Niederschlag" for unknown or empty input', () => {
    expect(translatePrecipitation('')).toBe('Kein Niederschlag');
    expect(translatePrecipitation('UNKNOWN')).toBe('Kein Niederschlag');
    expect(translatePrecipitation(null as unknown as string)).toBe('Kein Niederschlag');
    expect(translatePrecipitation(undefined as unknown as string)).toBe('Kein Niederschlag');
  });
});
