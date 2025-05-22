export interface WeatherReportDTO {
  minTemp: number;
  maxTemp: number;
  precip:
    | 'RAIN'
    | 'DRIZZLE'
    | 'SNOW'
    | 'SLEET'
    | 'HAIL'
    | 'FREEZING_RAIN'
    | 'FREEZING_DRIZZLE'
    | 'ICE_PELLETS'
    | 'GRAUPEL'
    | 'NONE';

  sunDuration: number | null;
  latitude: number;
  longitude: number;
  cityName: string;
}
