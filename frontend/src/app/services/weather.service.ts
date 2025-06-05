import { HttpClient } from '@angular/common/http';
import { Injectable } from '@angular/core';
import { Observable } from 'rxjs';
import { WeatherReportDTO } from '../interfaces/weather';

@Injectable({
  providedIn: 'root'
})
export class WeatherService {
  private readonly apiBaseUrl = 'http://localhost:8080/dailyweather';

  constructor(private http: HttpClient) { }

  private getYesterdayDate(): string {
    const yesterday = new Date();
    yesterday.setDate(yesterday.getDate() - 1);
    return yesterday.toISOString().split('T')[0];
  }

  getCachedWeatherReports(): Observable<WeatherReportDTO[]> {
    const date = this.getYesterdayDate();
    const url = `${this.apiBaseUrl}/cached?actualDate=${date}`;
    return this.http.get<WeatherReportDTO[]>(url);
  }

  getWeatherReportByCoords(
    cityName: string,
    latitude: number,
    longitude: number,
    date: string
  ): Observable<WeatherReportDTO> {
    const params = new URLSearchParams({
      cityName,
      longitude: longitude.toString(),
      latitude: latitude.toString(),
      actualDate: date,
    });
    return this.http.get<WeatherReportDTO>(
      `http://localhost:8080/dailyweather?${params.toString()}`
    );
  }
}
