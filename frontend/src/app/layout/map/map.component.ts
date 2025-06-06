import { AfterViewInit, Component, OnDestroy } from '@angular/core';
import * as L from 'leaflet';
import { HttpClient } from '@angular/common/http';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { GeoJsonObject } from 'geojson';
import { MapService } from '../../services/map.service';
import { MosquitoService } from '../../services/mosquito.service';
import { MosquitoOccurrence } from '../../interfaces/mosquito-occurrence.interface';
import { SelectionService } from '../../services/selection.service';
import { WeatherService } from '../../services/weather.service';
import { WeatherReportDTO } from '../../interfaces/weather';
import { translatePrecipitation } from '../../utils/precipitation-translator';
import { createTemperaturePinSvg } from '../../utils/temperature-pins';
import { LensSelectorComponent } from "../lens-selector/lens-selector.component";

@Component({
  selector: 'app-map',
  standalone: true,
  imports: [CommonModule, FormsModule, LensSelectorComponent],
  templateUrl: './map.component.html',
  styleUrls: ['./map.component.scss']
})
export class MapComponent implements AfterViewInit, OnDestroy {
  hasError = false;
  errorMessages: string[] = [];

  private mosquitoLayer = L.layerGroup();
  private temperatureLayer = L.layerGroup();

  public map!: L.Map;
  private selectedTempMarker: L.Marker | null = null;
  private central: L.LatLngExpression = [47.75, 13.0];
  private bounds: L.LatLngBoundsExpression = [
    [44.5, 6.5],
    [51.0, 20.5]
  ];

  private mosquitoIcon = L.icon({
    iconUrl: 'assets/img/marker-icon-2x-mosquito.png',
    iconSize: [90, 90],
    iconAnchor: [45, 90],
    popupAnchor: [0, -90],
    shadowUrl: 'assets/img/marker-shadow.png',
    shadowSize: [90, 90],
  });

  private higlightedMosquitoIcon = L.icon({
    iconUrl: 'assets/img/marker-icon-2x-mosquito-highlighted.png',
    iconSize: [90, 90],
    iconAnchor: [45, 90],
    popupAnchor: [0, -90],
    shadowUrl: 'assets/img/marker-shadow.png',
    shadowSize: [90, 90],
  });

  private selectedMarker: L.Marker | null = null;

  constructor(
    private http: HttpClient,
    private mapService: MapService,
    private mosquitoService: MosquitoService,
    private selectionService: SelectionService,
    private weatherService: WeatherService
  ) { }

  ngAfterViewInit(): void {
    // eslint-disable-next-line
    delete (L.Icon.Default.prototype as any)._getIconUrl;
    this.initMap();
    L.Icon.Default.mergeOptions({
      iconRetinaUrl: 'assets/img/marker-icon-2x.png',
      iconUrl: 'assets/img/marker-icon-2x.png',
      shadowUrl: 'assets/img/marker-shadow.png'
    });
    this.loadRegions();
  }

  ngOnDestroy(): void {
    if (this.map) {
      this.map.remove();
    }
  }

  private initMap(): void {
    if (this.map) {
      this.map.remove();
    }
    const isMobile = window.innerWidth <= 600;

    this.map = L.map('map', {
      center: this.central,
      zoom: isMobile ? 6 : 7,
      minZoom: isMobile ? 6 : 8,
      maxZoom: 15,
      maxBounds: this.bounds,
      zoomControl: false,
      scrollWheelZoom: true,
      touchZoom: isMobile,
      doubleClickZoom: false,
      maxBoundsViscosity: 1.0
    });

    L.control.zoom({ position: 'topright' }).addTo(this.map);

    this.map.createPane('temperaturePane');
    this.map.getPane('temperaturePane')!.style.zIndex = '650';

    this.map.createPane('mosquitoPane');
    this.map.getPane('mosquitoPane')!.style.zIndex = '600';

    this.mapService.setMap(this.map);

    L.tileLayer('https://tile.openstreetmap.de/{z}/{x}/{y}.png', {
      maxZoom: 18,
      attribution: 'Map data © <a href="https://openstreetmap.org">OpenStreetMap</a> contributors'
    }).addTo(this.map);
  }

  private loadRegions(): void {
    this.http.get<GeoJsonObject>('assets/austria-regions.geojson').subscribe((geojson) => {
      L.geoJSON(geojson, {
        style: () => ({
          color: 'blue',
          weight: 3,
          fillColor: 'rgba(0, 100, 255, 0.1)',
          fillOpacity: 0.4
        }),
        onEachFeature: (feature, layer) => {
          const name = feature.properties?.['NAME_1'];
          if (name) {
            layer.bindPopup(name);
          }
        }
      }).addTo(this.map);
    });
  }

  private loadWeatherMarkers(): void {
    this.temperatureLayer.clearLayers();

    this.weatherService.getCachedWeatherReports().subscribe({
      next: (reports: WeatherReportDTO[]) => {
        this.errorMessages = [];
        this.hasError = false;
        reports.forEach(report => {
          const icon = L.divIcon({
            className: '',
            html: createTemperaturePinSvg(report.maxTemp),
            iconSize: [50, 70],
            iconAnchor: [25, 70],
            popupAnchor: [0, -70]
          });

          const marker = L.marker([report.latitude, report.longitude], { icon, pane: 'temperaturePane' })
            .bindPopup(`
              <div style="user-select: none; -webkit-user-select: none; -moz-user-select: none; -ms-user-select: none;">
                <strong>Ort:</strong> ${report.cityName} <br>
                <strong>Min. Temperatur:</strong> ${report.minTemp} °C<br>
                <strong>Max. Temperatur:</strong> ${report.maxTemp} °C<br>
                <strong>Niederschlag:</strong> ${translatePrecipitation(report.precip)}
              </div>
            `)
            .on('click', () => {
              if (this.selectedTempMarker) {
                const oldReport = this.selectedTempMarker.getLatLng();
                const old = reports.find(r => r.latitude === oldReport.lat && r.longitude === oldReport.lng);
                if (old) {
                  this.selectedTempMarker.setIcon(L.divIcon({
                    className: '',
                    html: createTemperaturePinSvg(old.maxTemp),
                    iconSize: [50, 70],
                    iconAnchor: [25, 70],
                    popupAnchor: [0, -70],
                    pane: 'temperaturePane'
                  }));
                }
              }

              marker.setIcon(L.divIcon({
                className: '',
                html: createTemperaturePinSvg(report.maxTemp, true),
                iconSize: [50, 70],
                iconAnchor: [25, 70],
                popupAnchor: [0, -70],
                pane: 'temperaturePane'
              }));

              this.selectedTempMarker = marker;

              this.selectionService.setSelectedWeatherReport(report);
            })
            .on('popupclose', () => {
              if (this.selectedTempMarker === marker) {
                marker.setIcon(L.divIcon({
                  className: '',
                  html: createTemperaturePinSvg(report.maxTemp),
                  iconSize: [50, 70],
                  iconAnchor: [25, 70],
                  popupAnchor: [0, -70],
                  pane: 'temperaturePane'
                }));
                this.selectedTempMarker = null;
              }
            });

          this.temperatureLayer.addLayer(marker);
        });
        this.map.addLayer(this.temperatureLayer);
      },
      error: (err) => {
        console.error('Fehler beim Laden der Wetterdaten:', err);
        this.hasError = true;
        this.errorMessages = [];

        if (err.error && err.error.errors) {
          this.errorMessages = err.error.errors;
        } else if (err.status === 204) {
          this.errorMessages = ['No cached weather data available.'];
        } else if (err.status === 0) {
          this.errorMessages = ['Could not connect to the server.'];
        } else {
          this.errorMessages = ['An unknown error occurred.'];
        }
      }
    });
  }

  private loadMosquitoMarkers(): void {
    this.mosquitoLayer.clearLayers();

    this.mosquitoService.getMosquitoOccurrences().subscribe({
      next: (occurrences: MosquitoOccurrence[]) => {
        occurrences.forEach(occurrence => {
          const marker = L.marker([occurrence.latitude, occurrence.longitude], { icon: this.mosquitoIcon, pane: 'mosquitoPane' })
            .bindPopup(`
              <div style="user-select: none; -webkit-user-select: none; -moz-user-select: none; -ms-user-select: none;">
                <strong>Spezies:</strong> ${occurrence.species}<br>
                <strong>Datum:</strong> ${this.formatDate(occurrence.eventDate)}
              </div>
            `)
            .on('click', () => {
              if (this.selectedMarker) {
                this.selectedMarker.setIcon(this.mosquitoIcon);
              }

              marker.setIcon(this.higlightedMosquitoIcon);
              this.selectedMarker = marker;

              this.selectionService.setSelectedOccurrence(occurrence);
            })
            .on('popupclose', () => {
              if (this.selectedMarker === marker && this.map.hasLayer(marker)) {
                marker.setIcon(this.mosquitoIcon);
                this.selectedMarker = null;
              }
              this.selectionService.setSelectedOccurrence(null);
            });

          this.mosquitoLayer.addLayer(marker);
        });
        this.map.addLayer(this.mosquitoLayer);
        this.hasError = false;
      },
      error: (err) => {
        console.error('Error loading mosquito data:', err);
        this.hasError = true;
        this.errorMessages = [];

        if (err.error && err.error.errors) {
          this.errorMessages = err.error.errors;
        } else if (err.status === 204) {
          this.errorMessages = ['No cached weather data available.'];
        } else if (err.status === 0) {
          this.errorMessages = ['Could not connect to the server.'];
        } else {
          this.errorMessages = ['An unknown error occurred.'];
        }
      }
    });
  }

  private formatDate(dateStr: string): string {
    if (!dateStr || dateStr === 'Unknown') return 'Unknown Datum';

    const date = new Date(dateStr);
    if (isNaN(date.getTime())) return 'Unknown Datum';

    const day = String(date.getDate()).padStart(2, '0');
    const month = String(date.getMonth() + 1).padStart(2, '0');
    const year = date.getFullYear();

    return `${day}.${month}.${year}`;
  }

  closePopup() {
    this.hasError = false;
    this.errorMessages = [];
  }

  onLensSelected(lens: 'mosquito' | 'temperature'): void {
    if (this.selectedMarker && this.map.hasLayer(this.selectedMarker)) {
      this.map.removeLayer(this.selectedMarker);
    }
    this.selectedMarker = null;

    if (this.selectedTempMarker) {
      this.selectedTempMarker.setIcon(
        L.divIcon({
          className: '',
          html: createTemperaturePinSvg(0),
          iconSize: [50, 70],
          iconAnchor: [25, 70],
          popupAnchor: [0, -70],
          pane: 'temperaturePane'
        })
      );
      this.selectedTempMarker = null;
    }

    if (this.map.hasLayer(this.mosquitoLayer)) {
      this.map.removeLayer(this.mosquitoLayer);
    }
    if (this.map.hasLayer(this.temperatureLayer)) {
      this.map.removeLayer(this.temperatureLayer);
    }

    this.mosquitoLayer.clearLayers();
    this.temperatureLayer.clearLayers();

    if (lens === 'mosquito') {
      this.loadMosquitoMarkers();
    } else if (lens === 'temperature') {
      this.loadWeatherMarkers();
    }
  }
}