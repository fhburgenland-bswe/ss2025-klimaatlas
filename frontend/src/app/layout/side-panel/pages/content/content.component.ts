import { Component, Input, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { City } from '../../../../interfaces/city-interface';
import { MapService } from '../../../../services/map.service';
import * as L from 'leaflet';
import { MosquitoOccurrence } from '../../../../interfaces/mosquito-occurrence.interface';
import { HttpClient, HttpClientModule } from '@angular/common/http';
import { WeatherService } from '../../../../services/weather.service';
import { WeatherReportDTO } from '../../../../interfaces/weather';
import { translatePrecipitation } from '../../../../utils/precipitation-translator';
import { SelectionService } from '../../../../services/selection.service';

@Component({
  selector: 'app-content',
  imports: [CommonModule, FormsModule, HttpClientModule],
  templateUrl: './content.component.html',
  styleUrl: './content.component.scss',
})
export class ContentComponent implements OnInit {
  @Input() isCollapsed = false;
  searchTerm = '';
  cities: City[] = [];
  filteredCities: City[] = [];
  selectedMarker?: L.Marker;
  healthRiskContent = '';
  healthStatusContent = '';
  weatherInfo: {
    cityName: string;
    minTemp: number;
    maxTemp: number;
    precipitation: string;
  } | null = null;


  @Input() data: MosquitoOccurrence | null = null;

  constructor(private mapService: MapService, private http: HttpClient, public weatherService: WeatherService, private selectionService: SelectionService) { }

  ngOnInit(): void {
    this.mapService.getCities().subscribe(data => {
      this.cities = data;
    });

    this.selectionService.selectedWeatherReport$.subscribe(report => {
      if (report) {
        this.weatherInfo = {
          cityName: report.cityName,
          minTemp: report.minTemp,
          maxTemp: report.maxTemp,
          precipitation: translatePrecipitation(report.precip),
        };
      } else {
        this.weatherInfo = null;
      }
    });

    this.loadHealthData();
  }

  loadHealthData() {
    this.loadHealthRiskData();
    this.loadHealthStatusData();
  }

  loadTemperatureData() {
    this.weatherService.getCachedWeatherReports().subscribe({
      next: (reports: WeatherReportDTO[]) => {
        if (reports.length > 0) {
          const report = reports[0];
          this.selectionService.setSelectedWeatherReport(report);
          this.weatherInfo = {
            cityName: report.cityName,
            minTemp: report.minTemp,
            maxTemp: report.maxTemp,
            precipitation: translatePrecipitation(report.precip)
          };
        } else {
          this.selectionService.setSelectedWeatherReport(null);
          this.weatherInfo = null;
        }
      },
      error: () => {
        this.selectionService.setSelectedWeatherReport(null);
        this.weatherInfo = null;
      }
    });
  }

  loadHealthStatusData() {
    this.http.get('http://localhost:8081/load-status.php', { responseType: 'text' })
      .subscribe({
        next: data => this.healthStatusContent = data,
        error: err => {
          console.error('Failure when loading the Health Status content', err);
          this.healthStatusContent = 'Health Status Content is not available at the moment.'
        }
      })
  }

  loadHealthRiskData() {
    this.http.get('http://localhost:8081/load-risk.php', { responseType: 'text' })
      .subscribe({
        next: data => this.healthRiskContent = data,
        error: err => {
          console.error('Failure when loading the Health Risk content', err);
          this.healthRiskContent = 'Health Risk Content is not available at the moment.'
        }
      })
  }

  togglePanel(): void {
    this.isCollapsed = !this.isCollapsed;
  }

  onSearchChange(): void {
    const term = this.searchTerm.toLowerCase();
    if (term.length < 2) {
      this.filteredCities = [];
      return;
    }

    this.filteredCities = this.cities.filter(p =>
      p.place.toLowerCase().includes(term) || p.zipcode.includes(term)
    );
  }

  private getYesterdayDate(): string {
    const yesterday = new Date();
    yesterday.setDate(yesterday.getDate() - 1);
    return yesterday.toISOString().split('T')[0];
  }

  selectPlace(city: City): void {
    const lat = parseFloat(city.latitude);
    const lon = parseFloat(city.longitude);
    const map = this.mapService.getMap();
    const date = this.getYesterdayDate();
    if (!map) return;

    map.setView([lat, lon], 15);

    if (this.selectedMarker) {
      map.removeLayer(this.selectedMarker);
    }

    this.selectedMarker = L.marker([lat, lon])
      .addTo(map)
      .bindPopup(`${city.place} (${city.zipcode})`)
      .openPopup();

    this.filteredCities = [];
    this.searchTerm = `${city.place} (${city.zipcode})`;
    this.mapService.loadDistricts(city);

    this.weatherService.getCachedWeatherReports().subscribe({
      next: (reports: WeatherReportDTO[]) => {
        const matchedReport = reports.find(r =>
          r.cityName.toLowerCase() === city.place.toLowerCase()
        );

        if (matchedReport) {
          this.selectionService.setSelectedWeatherReport(matchedReport);
          this.weatherInfo = {
            cityName: matchedReport.cityName,
            minTemp: matchedReport.minTemp,
            maxTemp: matchedReport.maxTemp,
            precipitation: translatePrecipitation(matchedReport.precip)
          };
        } else {
          this.weatherService.getWeatherReportByCoords(city.place, lat, lon, date).subscribe({
            next: report => {
              this.selectionService.setSelectedWeatherReport(report);
              this.weatherInfo = {
                cityName: report.cityName,
                minTemp: report.minTemp,
                maxTemp: report.maxTemp,
                precipitation: translatePrecipitation(report.precip)
              };
            },
            error: err => {
              console.error("NO DATA AVAILABLE", err);
              this.selectionService.setSelectedWeatherReport(null);
              this.weatherInfo = null;
            }
          });
        }
      },
      error: err => {
        console.error("NO DATA AVAILABLE", err);
        this.selectionService.setSelectedWeatherReport(null);
        this.weatherInfo = null;
      }
    });
  }

  onEnterKey(event: KeyboardEvent): void {
    if (event.key === 'Enter') {
      const term = this.searchTerm.toLowerCase();
      const matchedCity = this.cities.find(p =>
        p.place.toLowerCase() === term || p.zipcode === term
      );

      if (matchedCity) {
        this.selectPlace(matchedCity);
      }
    }
  }
}

