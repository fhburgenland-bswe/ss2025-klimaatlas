import { ComponentFixture, TestBed } from '@angular/core/testing';

import { HomeComponent } from './home.component';
import { City } from '../interfaces/city-interface';
import { HttpClientTestingModule, HttpTestingController } from '@angular/common/http/testing';
import { FormsModule } from '@angular/forms';
import { CommonModule } from '@angular/common';
import L from 'leaflet';
import { FeatureCollection, Feature, Geometry } from 'geojson';

describe('HomeComponent', () => {
  let component: HomeComponent;
  let fixture: ComponentFixture<HomeComponent>;
  let httpMock: HttpTestingController;

  const mockCities: City[] = [
    {
      place: 'Wien', zipcode: '1010', latitude: '48.2082', longitude: '16.3738',
      country_code: '',
      state: '',
      state_code: '',
      province: '',
      province_code: '',
      community: '',
      community_code: ''
    },
    {
      place: 'Graz', zipcode: '8010', latitude: '47.0707', longitude: '15.4395',
      country_code: '',
      state: '',
      state_code: '',
      province: '',
      province_code: '',
      community: '',
      community_code: ''
    }
  ];

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [HomeComponent, HttpClientTestingModule, FormsModule, CommonModule]
    })
    .compileComponents();

    fixture = TestBed.createComponent(HomeComponent);
    component = fixture.componentInstance;
    httpMock = TestBed.inject(HttpTestingController);

    // stub out Leaflet.marker so it doesn't call map.addLayer()
    const fakeLayer = { bindPopup: () => ({ openPopup: () => { void 0; } }) };
    const fakeMarker = { addTo: () => fakeLayer };
    spyOn(L, 'marker').and.returnValue(fakeMarker as unknown as L.Marker);

    // mock data
    component.cities = mockCities;
    fixture.detectChanges();
    
    // 1. cities.json from ngOnInit
    httpMock.expectOne('assets/cities.json').flush(mockCities);

    // 2. austria-regions.geojson from loadRegions()
    const dummyGeo: GeoJSON.FeatureCollection = {
      type: 'FeatureCollection',
      features: []
    };
    
    httpMock
      .expectOne('assets/austria-regions.geojson')
      .flush(dummyGeo as GeoJSON.FeatureCollection);
  });

  afterEach(() => {
    httpMock.verify();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });

  describe('ngOnInit', () => {
    it('should fetch cities from JSON and assign to component.cities', () => {
      component.cities = [];
      component.ngOnInit();

      const req = httpMock.expectOne('assets/cities.json');
      expect(req.request.method).toBe('GET');
      req.flush(mockCities);

      expect(component.cities).toEqual(mockCities);
    });
  });

  describe('onSearchChange', () => {
    it('should clear filteredCities if searchTerm is less than 2 characters', () => {
      component.searchTerm = 'w';
      component.onSearchChange();
      expect(component.filteredCities.length).toBe(0);
    });

    it('should filter cities by place name or zipcode', () => {
      component.searchTerm = 'wi';
      component.onSearchChange();
      expect(component.filteredCities.length).toBe(1);
      expect(component.filteredCities[0].place).toBe('Wien');

      component.searchTerm = '8010';
      component.onSearchChange();
      expect(component.filteredCities[0].place).toBe('Graz');
    });
  });

  describe('onEnterKey', () => {
    it('should select matched city by name', () => {
      const spy = spyOn(component, 'selectPlace');
      component.searchTerm = 'Graz';

      const event = new KeyboardEvent('keydown', { key: 'Enter' });
      component.onEnterKey(event);
      expect(spy).toHaveBeenCalledOnceWith(mockCities[1]);
    });

    it('should select matched city by zipcode', () => {
      const spy = spyOn(component, 'selectPlace');
      component.searchTerm = '8010';

      const event = new KeyboardEvent('keydown', { key: 'Enter' });
      component.onEnterKey(event);
      expect(spy).toHaveBeenCalledOnceWith(mockCities[1]);
    });

    it('should not select if no match', () => {
      const spy = spyOn(component, 'selectPlace');
      component.searchTerm = 'unknown';
      const event = new KeyboardEvent('keydown', { key: 'Enter' });

      component.onEnterKey(event);
      expect(spy).not.toHaveBeenCalled();
    });
  });

  describe('loadRegions (standalone)', () => {
    it('should fetch geojson and call L.geoJSON', () => {
      spyOn(L, 'geoJSON').and.callThrough();
      component.map = jasmine.createSpyObj('map', ['addLayer']);
      component.loadRegions();
  
      const req = httpMock.expectOne('assets/austria-regions.geojson');
  
      const emptyGeoJson: FeatureCollection = {
        type: 'FeatureCollection',
        features: []
      };
      
      req.flush(emptyGeoJson);
  
      expect(L.geoJSON).toHaveBeenCalledWith(jasmine.any(Object), jasmine.any(Object));
    });
  });

  describe('selectPlace', () => {
    beforeEach(() => {
      // always give selectPlace a fake map to avoid Leaflet internals
      component.map = jasmine.createSpyObj('map', ['setView', 'removeLayer']);
      component.selectedMarker = undefined;
    });

    it('should remove previously selected marker when one exists', () => {
      const oldMarker = {} as L.Marker;
      component.selectedMarker = oldMarker;

      const targetCity = mockCities[1];
      component.selectPlace(targetCity);

      expect(component.map.removeLayer).toHaveBeenCalledWith(oldMarker);
    });

    it('should set view and update searchTerm + clear filteredCities', () => {
      const targetCity = mockCities[0];
      component.filteredCities = [mockCities[1]];
      component.selectPlace(targetCity);

      // verify map centering
      expect(component.map.setView)
        .toHaveBeenCalledWith([parseFloat(targetCity.latitude), parseFloat(targetCity.longitude)], 12);

      // verify searchTerm and dropdown cleared
      expect(component.searchTerm).toBe(`${targetCity.place} (${targetCity.zipcode})`);
      expect(component.filteredCities.length).toBe(0);
    });
  });

  describe('loadRegions popup binding', () => {
    it('should bind popup when feature has NAME_1 property', () => {
      // Spy on geoJSON to intercept options
      let onEachFeatureFn: (feature: Feature, layer: L.Layer) => void;
      // eslint-disable-next-line
      spyOn(L, 'geoJSON').and.callFake((_data, options: any) => {
        onEachFeatureFn = options.onEachFeature;
        // Return an object with addTo method to satisfy chain
        // eslint-disable-next-line
        return { addTo: () => {} } as any;
      });
      // Provide a test map
      component.map = jasmine.createSpyObj('map', ['addLayer']);

      // Call loadRegions
      component.loadRegions();

      // Flush HTTP request
      const req = httpMock.expectOne('assets/austria-regions.geojson');
      req.flush({
        type: 'FeatureCollection',
        features: [
          {
            type: 'Feature',
            properties: { NAME_1: 'Burgenland' },
            geometry: { type: 'Polygon', coordinates: [] } as Geometry
          }
        ]
      } as FeatureCollection<Geometry>);

      const fakeLayer = jasmine.createSpyObj('layer', ['bindPopup']);
      onEachFeatureFn!({
        type: 'Feature',
        properties: { NAME_1: 'Burgenland' },
        geometry: { type: 'Polygon', coordinates: [] }
      }, fakeLayer);

      expect(fakeLayer.bindPopup).toHaveBeenCalledWith('Burgenland');
    });

    it('should not bind popup when feature has no NAME_1', () => {
      // eslint-disable-next-line
      let onEachFeatureFn: (feat: any, layer: any) => void;
      // eslint-disable-next-line
      spyOn(L, 'geoJSON').and.callFake((_data, options: any) => {
        onEachFeatureFn = options.onEachFeature;
        // eslint-disable-next-line
        return { addTo: () => {} } as any;
      });
      component.map = jasmine.createSpyObj('map', ['addLayer']);

      component.loadRegions();
      const req = httpMock.expectOne('assets/austria-regions.geojson');
      req.flush({
        type: 'FeatureCollection',
        features: [
          {
            type: 'Feature',
            properties: {},
            geometry: {
              type: 'Polygon',
              coordinates: [[]]
            }
          }
        ]
      } as GeoJSON.FeatureCollection);

      const fakeLayer = jasmine.createSpyObj('layer', ['bindPopup']);
      onEachFeatureFn!({ properties: {} }, fakeLayer);

      expect(fakeLayer.bindPopup).not.toHaveBeenCalled();
    });
  });

  describe('loadRegions style function', () => {
    it('should use the provided style options for each feature', () => {
      // eslint-disable-next-line
      spyOn(L, 'geoJSON').and.callFake((_data, options: any) => {
        // eslint-disable-next-line
        const styleResult = options.style({} as any);
        expect(styleResult).toEqual({
          color: 'blue',
          weight: 3,
          fillColor: 'rgba(0, 100, 255, 0.1)',
          fillOpacity: 0.4
        });
        // eslint-disable-next-line
        return { addTo: () => {} } as any;
      });
      component.map = jasmine.createSpyObj('map', ['addLayer']);
      component.loadRegions();
      const req = httpMock.expectOne('assets/austria-regions.geojson');
      req.flush({
        type: 'FeatureCollection',
        features: []
      } as FeatureCollection<Geometry>);
    });
  });
});