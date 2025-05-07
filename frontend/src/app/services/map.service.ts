import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import * as L from 'leaflet';
import { City } from '../interfaces/city-interface';
import { Observable } from 'rxjs';

@Injectable({
  providedIn: 'root'
})
export class MapService {
  private map?: L.Map;
  private bezirkLayer?: L.GeoJSON;
  public cities: City[] = [];

  constructor(private http: HttpClient) { }

  setMap(map: L.Map): void {
    this.map = map;
  }

  getMap(): L.Map | undefined {
    return this.map;
  }

  getCities(): Observable<City[]> {
    return this.http.get<City[]>('assets/cities.json');
  }

  loadDistricts(city: City): void {
    const provinceCode = city.community_code || '';
    if (!this.map) return;

    this.http.get<GeoJSON.FeatureCollection | GeoJSON.FeatureCollection[]>('assets/district.geojson')
      .subscribe((geojson) => {
        if (this.bezirkLayer) {
          this.map!.removeLayer(this.bezirkLayer);
        }

        if (this.isFeatureCollection(geojson)) {
          this.addGeoJSONFeaturesToMap(geojson.features, provinceCode);
        } else if (Array.isArray(geojson)) {
          geojson.forEach(obj => {
            if (this.isFeatureCollection(obj)) {
              this.addGeoJSONFeaturesToMap(obj.features, provinceCode);
            }
          });
        }
      });
  }

  private isFeatureCollection(
    geojson: GeoJSON.FeatureCollection | GeoJSON.FeatureCollection[]
  ): geojson is GeoJSON.FeatureCollection {
    return Array.isArray((geojson as GeoJSON.FeatureCollection).features);
  }

  private addGeoJSONFeaturesToMap(features: GeoJSON.Feature[], provinceCode: string): void {
    if (!this.map) return;

    const filteredFeatures = features.filter(feature =>
      feature.properties?.['iso']?.startsWith(provinceCode)
    );

    if (filteredFeatures.length === 0) return;

    this.bezirkLayer = L.geoJSON(filteredFeatures, {
      style: () => ({
        color: 'red',
        weight: 4,
        fillColor: 'rgba(255,0,0,0.4)',
        fillOpacity: 0.6
      }),
      onEachFeature: (feature, layer) => {
        if (feature.properties?.name) {
          layer.bindPopup(feature.properties.name);
        }
      }
    }).addTo(this.map);
  }
}
