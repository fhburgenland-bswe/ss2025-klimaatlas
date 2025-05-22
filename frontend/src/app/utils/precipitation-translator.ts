/**
 * Maps precipitation string literals to German display names.
 */
export function translatePrecipitation(precip: string): string {
  switch (precip) {
    case 'RAIN':
      return 'Regen';
    case 'DRIZZLE':
      return 'Nieselregen';
    case 'SNOW':
      return 'Schnee';
    case 'SLEET':
      return 'Schneeregen';
    case 'HAIL':
      return 'Hagel';
    case 'FREEZING_RAIN':
      return 'gefrierender Regen';
    case 'FREEZING_DRIZZLE':
      return 'gefrierender Nieselregen';
    case 'ICE_PELLETS':
      return 'Eiskörner';
    case 'GRAUPEL':
      return 'Graupel';
    case 'NONE':
    default:
      return 'Kein Niederschlag';
  }
}