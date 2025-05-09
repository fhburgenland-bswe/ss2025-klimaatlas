import { AfterViewInit, Component, OnDestroy } from '@angular/core';
import * as L from 'leaflet';
import { HttpClient } from '@angular/common/http';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { GeoJsonObject } from 'geojson';
import { MapService } from '../../services/map.service';

@Component({
  selector: 'app-map',
  standalone: true,
  imports: [CommonModule, FormsModule],
  templateUrl: './map.component.html',
  styleUrls: ['./map.component.scss']
})
export class MapComponent implements AfterViewInit, OnDestroy {
  public map!: L.Map;
  private central: L.LatLngExpression = [47.75, 13.0];
  private bounds: L.LatLngBoundsExpression = [
    [44.5, 6.5],
    [51.0, 20.5]
  ];

  constructor(private http: HttpClient, private mapService: MapService) { }
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
}
