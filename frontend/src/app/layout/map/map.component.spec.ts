import { ComponentFixture, TestBed, fakeAsync, tick } from '@angular/core/testing';
import { MapComponent } from './map.component';
import { HttpClientTestingModule, HttpTestingController } from '@angular/common/http/testing';
import { MapService } from '../../services/map.service';
import * as L from 'leaflet';
import { MosquitoService } from '../../services/mosquito.service';
import { MosquitoOccurrence } from '../../interfaces/mosquito-occurrence.interface';
import { WeatherService } from '../../services/weather.service';
import { SelectionService } from '../../services/selection.service';
import { of, throwError } from 'rxjs';
import { WeatherReportDTO } from '../../interfaces/weather';

describe('MapComponent', () => {
  let component: MapComponent;
  let fixture: ComponentFixture<MapComponent>;
  let httpMock: HttpTestingController;
  let mapService: MapService;

  beforeAll(() => {
    spyOn(L.control, 'zoom').and.returnValue({
      addTo: jasmine.createSpy('addTo'),
      options: {},
      getPosition: () => 'topright',
      // eslint-disable-next-line @typescript-eslint/no-empty-function
      setPosition: () => { },
      getContainer: () => document.createElement('div'),
      // eslint-disable-next-line @typescript-eslint/no-empty-function
      remove: () => { }
    } as unknown as L.Control.Zoom);
  });

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [MapComponent, HttpClientTestingModule],
      providers: [
        {
          provide: MapService,
          useValue: {
            setMap: jasmine.createSpy('setMap')
          }
        },
        MosquitoService,
        SelectionService,
        WeatherService
      ]
    }).compileComponents();

    fixture = TestBed.createComponent(MapComponent);
    component = fixture.componentInstance;
    httpMock = TestBed.inject(HttpTestingController);
    mapService = TestBed.inject(MapService);
  });

  afterEach(() => {
    const container = document.getElementById('map');
    if (container) {
      container.remove();
    }
    httpMock.verify();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });

  it('should initialize map on ngAfterViewInit', fakeAsync(() => {
    spyOn(L, 'map').and.callThrough();

    fixture.detectChanges();

    const req = httpMock.expectOne('assets/austria-regions.geojson');
    req.flush({ type: 'FeatureCollection', features: [] });

    tick();

    expect(L.map).toHaveBeenCalled();
    expect(mapService.setMap).toHaveBeenCalled();
  }));

  it('should load and render GeoJSON regions', fakeAsync(() => {
    fixture.detectChanges();

    const req = httpMock.expectOne('assets/austria-regions.geojson');
    expect(req.request.method).toBe('GET');

    req.flush({
      type: 'FeatureCollection',
      features: [
        {
          type: 'Feature',
          properties: { NAME_1: 'Vienna' },
          geometry: {
            type: 'Polygon',
            coordinates: [[[16.3738, 48.2082], [16.4, 48.2], [16.35, 48.25], [16.3738, 48.2082]]]
          }
        }
      ]
    });

    tick();
  }));

  it('should call map.remove on destroy', () => {
    const removeSpy = jasmine.createSpy();
    component.map = { remove: removeSpy } as unknown as L.Map;

    component.ngOnDestroy();

    expect(removeSpy).toHaveBeenCalled();
  });

  it('should add markers for mosquito data', fakeAsync(() => {
    const validIcon = L.icon({
      iconUrl: 'data:image/png;base64,iVBORw0KGgoAAAANSUhEUgAAAAEAAAABCAQAAAC1HAwCAAAAC0lEQVR4nGNgYAAAAAMAASsJTYQAAAAASUVORK5CYII=',
      iconSize: [1, 1]
    });

    const marker = L.marker([48, 16], { icon: validIcon });

    spyOn(L, 'marker').and.returnValue(marker);
    spyOn(marker, 'bindPopup').and.callThrough();
    spyOn(marker, 'on').and.callThrough();

    fixture.detectChanges();
    component.onLensSelected('mosquito');
    httpMock.expectOne('assets/austria-regions.geojson').flush({ type: 'FeatureCollection', features: [] });

    const mockData: MosquitoOccurrence[] = [{
      species: 'Aedes testus',
      eventDate: '2024-01-01',
      latitude: 48,
      longitude: 16
    }];

    httpMock.expectOne('http://localhost:8080/mosquitoes').flush(mockData);
    tick();

    expect(marker.bindPopup).toHaveBeenCalled();
    expect(marker.on).toHaveBeenCalled();
  }));

  it('should handle marker click and popupclose', fakeAsync(() => {
    const originalMarkerFn = L.marker;

    spyOn(L, 'marker').and.callFake((latlng, options) => {
      const marker = originalMarkerFn(latlng, options);

      const callbacks: Record<string, () => void> = {};
      spyOn(marker, 'on').and.callFake(((event: string, cb: (e: L.LeafletEvent) => void): L.Marker => {
        if (event === 'click') {
          (marker as { clickCallback?: () => void }).clickCallback = () => cb({} as L.LeafletMouseEvent);
        }
        if (event === 'popupclose') {
          (marker as { popupCloseCallback?: () => void }).popupCloseCallback = () => cb({} as L.PopupEvent);
        }
        return marker;
      }) as unknown as typeof marker.on);

      spyOn(marker, 'bindPopup').and.callThrough();
      spyOn(marker, 'setIcon').and.callThrough();

      (marker as L.Marker & { clickCallback?: () => void }).clickCallback = () => callbacks['click']?.();
      (marker as L.Marker & { popupCloseCallback?: () => void }).popupCloseCallback = () => callbacks['popupclose']?.();

      return marker;
    });

    const mockOccurrence: MosquitoOccurrence = {
      species: 'Culex testicus',
      eventDate: '2024-05-01',
      latitude: 47,
      longitude: 16
    };

    const selectionSpy = spyOn(component['selectionService'], 'setSelectedOccurrence');

    fixture.detectChanges();
    component.onLensSelected('mosquito');

    httpMock.expectOne('assets/austria-regions.geojson').flush({ type: 'FeatureCollection', features: [] });
    httpMock.expectOne('http://localhost:8080/mosquitoes').flush([mockOccurrence]);
    tick();

    const markers = component['mosquitoLayer'].getLayers() as L.Marker[];
    // eslint-disable-next-line
    const marker = markers[0] as any;

    marker.clickCallback();
    marker.popupCloseCallback();

    expect(selectionSpy).toHaveBeenCalledTimes(2);
    expect(selectionSpy.calls.argsFor(0)[0]).toEqual(mockOccurrence);
    expect(selectionSpy.calls.argsFor(1)[0]).toBeNull();
  }));

  it('should set hasMosquitoError to true on mosquitoService error', fakeAsync(() => {
    spyOn(console, 'error');

    fixture.detectChanges();

    component.onLensSelected('mosquito');

    httpMock.expectOne('assets/austria-regions.geojson').flush({ type: 'FeatureCollection', features: [] });
    httpMock.expectOne('http://localhost:8080/mosquitoes').flush('Error', { status: 500, statusText: 'Server Error' });

    tick();

    expect(component.hasError).toBeTrue();
  }));

  it('should format valid date correctly', () => {
    const formatted = component['formatDate']('2024-01-01');
    expect(formatted).toBe('01.01.2024');
  });

  it('should return fallback text for unknown date string', () => {
    expect(component['formatDate']('Unknown')).toBe('Unknown Datum');
    expect(component['formatDate']('')).toBe('Unknown Datum');
    expect(component['formatDate']('invalid-date')).toBe('Unknown Datum');
  });

  it('should reset hasError on closePopup', () => {
    component.hasError = true;
    component.closePopup();
    expect(component.hasError).toBeFalse();
  });

  it('should call map.remove on destroy', () => {
    const removeSpy = jasmine.createSpy('remove');
    component.map = { remove: removeSpy } as unknown as L.Map;
    component.ngOnDestroy();
    expect(removeSpy).toHaveBeenCalled();
  });

  it('should create weather markers with icons and handle click and popupclose', fakeAsync(() => {
    const mockReports: WeatherReportDTO[] = [{
      cityName: 'Wien',
      latitude: 48.2,
      longitude: 16.3,
      minTemp: 5,
      maxTemp: 25,
      precip: 'RAIN',
      sunDuration: null
    }];

    spyOn(component['weatherService'], 'getCachedWeatherReports').and.returnValue(of(mockReports));

    const markerMock = jasmine.createSpyObj('marker', ['bindPopup', 'on', 'setIcon', 'addTo', 'getLatLng']);
    markerMock.bindPopup.and.returnValue(markerMock);
    markerMock.on.and.callFake((event: string, cb: () => void): L.Marker => {
      if (event === 'click') markerMock.clickCallback = cb;
      if (event === 'popupclose') markerMock.popupCloseCallback = cb;
      return markerMock;
    });
    markerMock.getLatLng.and.returnValue(L.latLng(48.2, 16.3));

    spyOn(L, 'marker').and.returnValue(markerMock);
    spyOn(L, 'divIcon').and.callFake((options?: L.DivIconOptions): L.DivIcon => {
      return {
        options: options || {},
      } as L.DivIcon;
    });

    component['map'] = {
      hasLayer: jasmine.createSpy('hasLayer').and.returnValue(false),
      removeLayer: jasmine.createSpy('removeLayer'),
      addLayer: jasmine.createSpy('addLayer'),
      getContainer: () => document.createElement('div'),
      scrollWheelZoom: {
        enable: jasmine.createSpy(),
        disable: jasmine.createSpy(),
      },
      on: jasmine.createSpy('on'),
      createPane: jasmine.createSpy('createPane'),
      getPane: jasmine.createSpy('getPane').and.returnValue(document.createElement('div')),
      remove: jasmine.createSpy('remove')
    } as unknown as L.Map;

    component.onLensSelected('temperature');

    tick();

    expect(L.marker).toHaveBeenCalledWith([48.2, 16.3], jasmine.objectContaining({
      icon: jasmine.anything(),
      pane: 'temperaturePane'
    }));

    markerMock.clickCallback();
    expect(markerMock.setIcon).toHaveBeenCalled();
    expect(component['selectedTempMarker']).toBe(markerMock);

    markerMock.popupCloseCallback();
    expect(markerMock.setIcon).toHaveBeenCalled();
    expect(component['selectedTempMarker']).toBeNull();

    expect(component['map'].addLayer).toHaveBeenCalledWith(component['temperatureLayer']);
  }));

  it('should remove selectedMarker and selectedTempMarker, and clear layers on lens switch', fakeAsync(() => {
    const mapMock = jasmine.createSpyObj('map', ['hasLayer', 'removeLayer', 'addLayer', 'getContainer', 'on', 'remove']);
    // eslint-disable-next-line @typescript-eslint/no-empty-function
    mapMock.scrollWheelZoom = { enable: () => { }, disable: () => { } };
    mapMock.getContainer.and.returnValue(document.createElement('div'));
    mapMock.on.and.returnValue(mapMock);

    component['map'] = mapMock as unknown as L.Map;

    const selectedMarker = {} as L.Marker;
    component['selectedMarker'] = selectedMarker;
    mapMock.hasLayer.withArgs(selectedMarker).and.returnValue(true);

    const selectedTempMarker = jasmine.createSpyObj('L.Marker', ['setIcon']);
    component['selectedTempMarker'] = selectedTempMarker;

    mapMock.hasLayer.withArgs(component['mosquitoLayer']).and.returnValue(true);
    mapMock.hasLayer.withArgs(component['temperatureLayer']).and.returnValue(true);

    component.onLensSelected('mosquito');

    httpMock.expectOne('http://localhost:8080/mosquitoes').flush([]);

    tick();

    expect(mapMock.removeLayer).toHaveBeenCalledWith(selectedMarker);
    expect(selectedTempMarker.setIcon).toHaveBeenCalled();
    expect(component['selectedTempMarker']).toBeNull();
    expect(mapMock.removeLayer).toHaveBeenCalledWith(component['mosquitoLayer']);
    expect(mapMock.removeLayer).toHaveBeenCalledWith(component['temperatureLayer']);
  }));
  
  it('should remove existing map if already initialized', () => {
    const mockMap = jasmine.createSpyObj<L.Map>('map', ['remove']);

    component['map'] = mockMap;

    (component as unknown as { initMap: () => void }).initMap();

    expect(mockMap.remove).toHaveBeenCalled();
  });

  it('should initialize map with zoom 6 and minZoom 6 for mobile', () => {
    spyOnProperty(window, 'innerWidth').and.returnValue(500); // mobil
    const mapSpy = spyOn(L, 'map').and.callThrough();

    component['initMap']();

    expect(mapSpy).toHaveBeenCalledWith('map', jasmine.objectContaining({
      zoom: 6,
      minZoom: 6,
      touchZoom: true
    }));
  });

  it('should initialize map with zoom 7 and minZoom 8 for desktop', () => {
    spyOnProperty(window, 'innerWidth').and.returnValue(1024); // desktop
    const mapSpy = spyOn(L, 'map').and.callThrough();

    component['initMap']();

    expect(mapSpy).toHaveBeenCalledWith('map', jasmine.objectContaining({
      zoom: 7,
      minZoom: 8,
      touchZoom: false
    }));
  });

})

describe('MapComponent - Weather Error Handling', () => {
  let component: MapComponent;
  let fixture: ComponentFixture<MapComponent>;
  let weatherService: jasmine.SpyObj<WeatherService>;

  beforeEach(async () => {
    const weatherSpy = jasmine.createSpyObj('WeatherService', ['getCachedWeatherReports']);

    await TestBed.configureTestingModule({
      imports: [HttpClientTestingModule, MapComponent],
      providers: [
        { provide: WeatherService, useValue: weatherSpy },
        MapService,
        MosquitoService,
        SelectionService
      ]
    }).compileComponents();

    fixture = TestBed.createComponent(MapComponent);
    component = fixture.componentInstance;
    weatherService = TestBed.inject(WeatherService) as jasmine.SpyObj<WeatherService>;

    component['map'] = {
      addLayer: () => { /* mock implementation */ },
      removeLayer: () => { /* mock implementation */ },
      hasLayer: () => false,
      getContainer: () => document.createElement('div'),
      scrollWheelZoom: {
        enable: () => { /* mock enable */ },
        disable: () => { /* mock disable */ },
      },
      on: () => component['map'],
      createPane: () => { /* mock createPane */ },
      getPane: () => document.createElement('div'),
      remove: () => { /* mock remove */ }
    } as unknown as L.Map;
  });

  it('should handle backend error with error.error.errors', () => {
    const mockError = {
      error: { errors: ['Something went wrong'] },
      status: 400
    };

    weatherService.getCachedWeatherReports.and.returnValue(throwError(() => mockError));

    component['loadWeatherMarkers']();

    expect(component.hasError).toBeTrue();
    expect(component.errorMessages).toEqual(['Something went wrong']);
  });

  it('should handle 204 No Content response', () => {
    const mockError = { status: 204 };

    weatherService.getCachedWeatherReports.and.returnValue(throwError(() => mockError));

    component['loadWeatherMarkers']();

    expect(component.hasError).toBeTrue();
    expect(component.errorMessages).toEqual(['No cached weather data available.']);
  });

  it('should handle network error (status 0)', () => {
    const mockError = { status: 0 };

    weatherService.getCachedWeatherReports.and.returnValue(throwError(() => mockError));

    component['loadWeatherMarkers']();

    expect(component.hasError).toBeTrue();
    expect(component.errorMessages).toEqual(['Could not connect to the server.']);
  });

  it('should handle unknown error', () => {
    const mockError = { status: 500 };

    weatherService.getCachedWeatherReports.and.returnValue(throwError(() => mockError));

    component['loadWeatherMarkers']();

    expect(component.hasError).toBeTrue();
    expect(component.errorMessages).toEqual(['An unknown error occurred.']);
  });
})
