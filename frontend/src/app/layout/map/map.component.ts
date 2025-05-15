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

@Component({
  selector: 'app-map',
  standalone: true,
  imports: [CommonModule, FormsModule],
  templateUrl: './map.component.html',
  styleUrls: ['./map.component.scss']
})
export class MapComponent implements AfterViewInit, OnDestroy {
  hasError = false;
  public map!: L.Map;
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
    private selectionService: SelectionService
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

    this.map.on('focus', () => {
      const container = this.map.getContainer();
      container.addEventListener('wheel', this.handleMapScroll.bind(this));
    });

    this.map.on('blur', () => {
      const container = this.map.getContainer();
      container.removeEventListener('wheel', this.handleMapScroll.bind(this));
    });

    this.loadRegions();
    this.loadMosquitoMarkers();
  }

  handleMapScroll(event: WheelEvent): void {
    if (event.ctrlKey) {
      this.map.scrollWheelZoom.enable();
    } else {
      this.map.scrollWheelZoom.disable();
    }
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
      scrollWheelZoom: false,
      touchZoom: isMobile,
      doubleClickZoom: false,
      maxBoundsViscosity: 1.0
    });

    L.control.zoom({ position: 'topright' }).addTo(this.map);

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

  private loadMosquitoMarkers(): void {
    this.mosquitoService.getMosquitoOccurrences().subscribe({
      next: (occurrences: MosquitoOccurrence[]) => {
        occurrences.forEach(occurrence => {
          const marker = L.marker([occurrence.latitude, occurrence.longitude], { icon: this.mosquitoIcon })
            .bindPopup(`
              <strong>Spezies:</strong> ${occurrence.species}<br>
              <strong>Datum:</strong> ${this.formatDate(occurrence.eventDate)}
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
              if (this.selectedMarker) {
                this.selectedMarker.setIcon(this.mosquitoIcon);
              }
              
              marker.setIcon(this.mosquitoIcon);
              this.selectedMarker = marker;

              this.selectionService.setSelectedOccurrence(null);
            });
  
          marker.addTo(this.map);
          this.hasError = false;
        });
      },
      error: (err) => {
        console.error('Error loading mosquito data:', err);
        this.hasError = true;
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
  }
}
