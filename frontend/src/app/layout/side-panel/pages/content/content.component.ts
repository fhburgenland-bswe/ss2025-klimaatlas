import { Component, Input, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { City } from '../../../../interfaces/city-interface';
import { MapService } from '../../../../services/map.service';
import * as L from 'leaflet';
import { MosquitoOccurrence } from '../../../../interfaces/mosquito-occurrence.interface';
import { HttpClient, HttpClientModule } from '@angular/common/http';

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

  @Input() data: MosquitoOccurrence | null = null;

  constructor(private mapService: MapService, private http: HttpClient) { }

  ngOnInit(): void {
    this.mapService.getCities().subscribe(data => {
      this.cities = data;
    });
    this.loadHealthData();
    this.loadHealthRiskData();
    this.loadHealthStatusData();
    console.log("oninit: " + this.healthRiskContent);
  }

  loadHealthData() {
    this.loadHealthRiskData();
    this.loadHealthStatusData();
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

  selectPlace(city: City): void {
    const lat = parseFloat(city.latitude);
    const lon = parseFloat(city.longitude);
    const map = this.mapService.getMap();
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
    console.log(city);

    this.mapService.loadDistricts(city);
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

