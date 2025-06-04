import { Injectable } from '@angular/core';
import { BehaviorSubject } from 'rxjs';
import { MosquitoOccurrence } from '../interfaces/mosquito-occurrence.interface';
import { WeatherReportDTO } from '../interfaces/weather';
@Injectable({
  providedIn: 'root'
})
export class SelectionService {
  private selectedOccurrenceSubject = new BehaviorSubject<MosquitoOccurrence | null>(null);
  selectedOccurrence$ = this.selectedOccurrenceSubject.asObservable();

  private selectedWeatherReportSubject = new BehaviorSubject<WeatherReportDTO | null>(null);
  selectedWeatherReport$ = this.selectedWeatherReportSubject.asObservable();

  setSelectedOccurrence(occurrence: MosquitoOccurrence | null) {
    this.selectedOccurrenceSubject.next(occurrence);
  }

  setSelectedWeatherReport(report: WeatherReportDTO | null) {
    this.selectedWeatherReportSubject.next(report);
  }
}
