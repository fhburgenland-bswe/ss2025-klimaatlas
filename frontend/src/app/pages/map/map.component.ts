import { AfterViewInit, Component, OnInit } from '@angular/core';
import * as L from 'leaflet';
import { HttpClient } from '@angular/common/http';
import { City } from '../../interfaces/city-interface';
import { FormsModule } from '@angular/forms';
import { CommonModule } from '@angular/common';
import { GeoJsonObject } from 'geojson';

@Component({
  selector: 'app-home',
  imports: [
    FormsModule,
    CommonModule
  ],
  templateUrl: './map.component.html',
  styleUrl: './map.component.scss'
})
export class MapComponent implements AfterViewInit, OnInit {
  public map!: L.Map;
  private central: L.LatLngExpression = [47.5162, 14.5501]
  private bounds: L.LatLngBoundsExpression = [
    [46.3588, 9.5300],   // SouthWest (Vorarlberg)
    [49.0391, 17.1600]   // NorthEast (Burgenland)
  ];
  cities: City[] = [];
  filteredCities: City[] = [];
  searchTerm = '';
  selectedMarker?: L.Marker;

  constructor(private http: HttpClient) { }

  ngAfterViewInit(): void {
    // Initializes the map and loads region data after the view is rendered
    this.initMap();
    // Enable scroll zoom only when holding CTRL

    this.map.on('focus', () => {
      const container = this.map.getContainer();

      container.addEventListener('wheel', (e: WheelEvent) => {
        if (e.ctrlKey) {
          this.map.scrollWheelZoom.enable();
        } else {
          this.map.scrollWheelZoom.disable();
        }
      });
    });
    this.loadRegions();
  }

  ngOnInit(): void {
    // Fetches city data from the JSON file when the component is initialized
    this.http.get<City[]>('assets/cities.json').subscribe(data => {
      this.cities = data;
    });
  }

  private initMap(): void {
    // Initializes the Leaflet map, setting up its center, zoom levels, and behavior based on whether the user is on mobile or not
    const isMobile = window.innerWidth <= 600;

    this.map = L.map('map', {
      center: this.central,
      zoom: isMobile ? 6 : 7,
      minZoom: isMobile ? 6 : 8,
      maxZoom: 12,
      maxBounds: this.bounds,
      scrollWheelZoom: false,
      touchZoom: isMobile,
      doubleClickZoom: false,
      maxBoundsViscosity: 1.0
    });

    const tiles = L.tileLayer('https://tile.openstreetmap.de/{z}/{x}/{y}.png', {
      maxZoom: 18,
      attribution: 'Map data © <a href="https://openstreetmap.org">OpenStreetMap</a> contributors'
    })

    tiles.addTo(this.map);
  }

  public loadRegions(): void {
    // Loads geojson data for regions and adds them to the map with custom styling
    this.http.get<GeoJsonObject>('assets/austria-regions.geojson').subscribe((geojson) => {
      L.geoJSON(geojson, {
        style: () => ({
          color: 'blue',
          weight: 3,
          fillColor: 'rgba(0, 100, 255, 0.1)',
          fillOpacity: 0.4
        }),
        onEachFeature: (feature, layer) => {
          // Bind a popup with region names
          if (feature.properties && feature.properties.NAME_1) {
            layer.bindPopup(`${feature.properties.NAME_1}`);
          }
        }
      }).addTo(this.map);
    });
  }

  onSearchChange(): void {
    // Filters cities based on the current search term, updating the filtered cities list
    const term = this.searchTerm.toLowerCase();
    if (term.length < 2) {
      this.filteredCities = []; // Clears the results if the search term is too short
      return;
    }

    // Filters cities by matching place name or zipcode
    this.filteredCities = this.cities.filter(p =>
      p.place.toLowerCase().includes(term) || p.zipcode.includes(term)
    );
  }

  selectPlace(city: City): void {
    // Selects a city by setting the map view to its coordinates and adding a marker for it
    const lat = parseFloat(city.latitude);
    const lon = parseFloat(city.longitude);
    this.map.setView([lat, lon], 12);

    if (this.selectedMarker) {
      this.map.removeLayer(this.selectedMarker); // Removes previously selected marker
    }

    // Adds a new marker with a popup displaying the city's name and zipcode
    this.selectedMarker = L.marker([lat, lon])
      .addTo(this.map)
      .bindPopup(`${city.place} (${city.zipcode})`)
      .openPopup();

    // Clears the filtered cities and updates the search term with the selected city's name and zipcode
    this.filteredCities = [];
    this.searchTerm = `${city.place} (${city.zipcode})`;
  }

  onEnterKey(event: KeyboardEvent): void {
    // Handles the Enter key press in the search input field, selecting a city if a match is found
    if (event.key === 'Enter') {
      const term = this.searchTerm.toLowerCase();
      const matchedCity = this.cities.find(p =>
        p.place.toLowerCase() === term || p.zipcode === term // Matches by city name or zipcode
      );

      if (matchedCity) {
        this.selectPlace(matchedCity); // Select the matched city
      }
    }
  }
}
