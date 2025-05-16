import { TestBed } from '@angular/core/testing';
import { HttpClientTestingModule, HttpTestingController } from '@angular/common/http/testing';
import * as L from 'leaflet';

import { MapService } from './map.service';
import { City } from '../interfaces/city-interface';

describe('MapService', () => {
  let service: MapService;
  let httpMock: HttpTestingController;
  let map: L.Map;

  beforeEach(() => {
    TestBed.configureTestingModule({
      imports: [HttpClientTestingModule],
      providers: [MapService]
    });
    service = TestBed.inject(MapService);
    httpMock = TestBed.inject(HttpTestingController);

    map = L.map(document.createElement('div'));
  });

  afterEach(() => {
    httpMock.verify();
  });

  it('should be created', () => {
    expect(service).toBeTruthy();
  });

  it('should set and get map', () => {
    service.setMap(map);
    expect(service.getMap()).toBe(map);
  });

  it('should getCities and return City array', () => {
    const dummyCities: City[] = [{
      place: 'City1', zipcode: '1234', community_code: '01',
      country_code: '',
      state: '',
      state_code: '',
      province: '',
      province_code: '',
      community: '',
      latitude: '',
      longitude: ''
    }];
    service.getCities().subscribe(cities => {
      expect(cities).toEqual(dummyCities);
    });

    const req = httpMock.expectOne('assets/cities.json');
    expect(req.request.method).toBe('GET');
    req.flush(dummyCities);
  });

  describe('loadDistricts', () => {
    const dummyCity = { community_code: '01' } as City;
    const dummyGeoJson: GeoJSON.FeatureCollection = {
      type: 'FeatureCollection',
      features: [
        {
          type: 'Feature',
          properties: { iso: '01A', name: 'District A' },
          geometry: { type: 'Point', coordinates: [0, 0] }
        }
      ]
    };

    it('should return immediately if no map set', () => {
      spyOn(service, 'getMap').and.returnValue(undefined);
      service.loadDistricts(dummyCity); // Should do nothing, no error
    });

    it('should remove existing bezirkLayer and add new one with filtered features', () => {
      service.setMap(map);

      const removeLayerSpy = spyOn(map, 'removeLayer').and.callThrough();

      // Set a dummy bezirkLayer to service to test removal
      (service as unknown as { bezirkLayer?: L.GeoJSON }).bezirkLayer = L.geoJSON();

      service.loadDistricts(dummyCity);

      const req = httpMock.expectOne('assets/district.geojson');
      expect(req.request.method).toBe('GET');

      req.flush(dummyGeoJson);

      expect(removeLayerSpy).toHaveBeenCalled();

      // Check that bezirkLayer is set

      expect((service as unknown as { bezirkLayer?: L.GeoJSON }).bezirkLayer).toBeDefined();
    });

    it('should handle GeoJSON array correctly', () => {
      service.setMap(map);

      const geoJsonArray: GeoJSON.FeatureCollection[] = [
        dummyGeoJson,
        {
          type: 'FeatureCollection',
          features: [
            {
              type: 'Feature',
              properties: { iso: '01B', name: 'District B' },
              geometry: { type: 'Point', coordinates: [1, 1] }
            }
          ]
        }
      ];

      service.loadDistricts(dummyCity);

      const req = httpMock.expectOne('assets/district.geojson');
      req.flush(geoJsonArray);

      expect((service as unknown as { bezirkLayer?: L.GeoJSON }).bezirkLayer).toBeDefined();
    });

    it('should not add layer if no features match', () => {
      service.setMap(map);

      const geoJsonNoMatch: GeoJSON.FeatureCollection = {
        type: 'FeatureCollection',
        features: [
          {
            type: 'Feature',
            properties: { iso: '99Z', name: 'District Z' },
            geometry: { type: 'Point', coordinates: [0, 0] }
          }
        ]
      };

      service.loadDistricts(dummyCity);

      const req = httpMock.expectOne('assets/district.geojson');
      req.flush(geoJsonNoMatch);

      expect(((service as unknown as { bezirkLayer?: L.GeoJSON }).bezirkLayer)).toBeUndefined();
    });
  });

  describe('isFeatureCollection', () => {
    it('should return true for FeatureCollection', () => {
      const featureCollection: GeoJSON.FeatureCollection = {
        type: 'FeatureCollection',
        features: []
      };

      const isFeatureCollection = (service as unknown as { isFeatureCollection: (data: unknown) => boolean }).isFeatureCollection;

      expect(isFeatureCollection(featureCollection)).toBeTrue();
    });

    it('should return false for array', () => {
      const arr: unknown = [];

      const isFeatureCollection = (service as unknown as { isFeatureCollection: (data: unknown) => boolean }).isFeatureCollection;

      expect(isFeatureCollection(arr)).toBeFalse();
    });
  });


  describe('addGeoJSONFeaturesToMap', () => {
    it('should return immediately if no map', () => {
      (service as unknown as { map?: L.Map }).map = undefined;
      (service as MapService & { addGeoJSONFeaturesToMap: (features: GeoJSON.Feature[], code: string) => void }).addGeoJSONFeaturesToMap([], '01');
    });

    it('should add filtered features layer', () => {
      service.setMap(map);
      const features: GeoJSON.Feature[] = [
        {
          type: 'Feature',
          properties: { iso: '01A', name: 'Test' },
          geometry: { type: 'Point', coordinates: [0, 0] }
        },
        {
          type: 'Feature',
          properties: { iso: '02B', name: 'NoMatch' },
          geometry: { type: 'Point', coordinates: [1, 1] }
        }
      ];

      (service as MapService & { addGeoJSONFeaturesToMap: (features: GeoJSON.Feature[], code: string) => void }).addGeoJSONFeaturesToMap(features, '01');
      expect((service as unknown as { bezirkLayer?: L.GeoJSON }).bezirkLayer).toBeDefined();
    });

    it('should return if filtered features empty', () => {
      service.setMap(map);
      const features: GeoJSON.Feature[] = [
        {
          type: 'Feature',
          properties: { iso: '99Z', name: 'NoMatch' },
          geometry: { type: 'Point', coordinates: [0, 0] }
        }
      ];

      (service as MapService & { addGeoJSONFeaturesToMap: (features: GeoJSON.Feature[], code: string) => void }).addGeoJSONFeaturesToMap(features, '01');
      expect((service as unknown as { bezirkLayer?: L.GeoJSON }).bezirkLayer).toBeUndefined();
    });
  });
});
