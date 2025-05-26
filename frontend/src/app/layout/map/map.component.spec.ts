import { ComponentFixture, TestBed, fakeAsync, tick } from '@angular/core/testing';
import { MapComponent } from './map.component';
import { HttpClientTestingModule, HttpTestingController } from '@angular/common/http/testing';
import { MapService } from '../../services/map.service';
import * as L from 'leaflet';
import { MosquitoService } from '../../services/mosquito.service';
import { MosquitoOccurrence } from '../../interfaces/mosquito-occurrence.interface';
import { WeatherService } from '../../services/weather.service';
import { SelectionService } from '../../services/selection.service';
import { of } from 'rxjs';
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
      setPosition: () => {},
      getContainer: () => document.createElement('div'),
      // eslint-disable-next-line @typescript-eslint/no-empty-function
      remove: () => {}
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

    const yesterday = new Date();
    yesterday.setDate(yesterday.getDate() - 1);
    const actualDateStr = yesterday.toISOString().split('T')[0];

    httpMock.expectOne(`http://localhost:8080/dailyweather/cached?actualDate=${actualDateStr}`).flush([]);

    const req = httpMock.expectOne('assets/austria-regions.geojson');
    req.flush({ type: 'FeatureCollection', features: [] });

    const mosquitoReq = httpMock.expectOne('http://localhost:8080/mosquitoes');
    mosquitoReq.flush([]);

    tick();

    expect(L.map).toHaveBeenCalled();
    expect(mapService.setMap).toHaveBeenCalled();
  }));

  it('should load and render GeoJSON regions', fakeAsync(() => {
    fixture.detectChanges();

    const yesterday = new Date();
    yesterday.setDate(yesterday.getDate() - 1);
    const actualDateStr = yesterday.toISOString().split('T')[0];

    httpMock.expectOne(`http://localhost:8080/dailyweather/cached?actualDate=${actualDateStr}`).flush([]);

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

    const mosquitoReq = httpMock.expectOne('http://localhost:8080/mosquitoes');
    mosquitoReq.flush([]);

    tick();
  }));

  it('should register focus and blur events', fakeAsync(() => {
    const onSpy = jasmine.createSpy('on');
    const fakeMap = {
      on: onSpy,
      getContainer: () => ({
        addEventListener: jasmine.createSpy(),
        removeEventListener: jasmine.createSpy()
      }),
      getMinZoom: () => 6,
      getMaxZoom: () => 18,
      remove: jasmine.createSpy('remove'),
      addLayer: jasmine.createSpy('addLayer'),
      createPane: jasmine.createSpy('createPane'),
      getPane: jasmine.createSpy('getPane').and.returnValue(document.createElement('div'))
    } as unknown as L.Map;
  
    spyOn(L, 'map').and.returnValue(fakeMap);
  
    fixture.detectChanges();
    tick();
  
    expect(onSpy).toHaveBeenCalledWith('focus', jasmine.any(Function));
    expect(onSpy).toHaveBeenCalledWith('blur', jasmine.any(Function));
  
    const yesterday = new Date();
    yesterday.setDate(yesterday.getDate() - 1);
    const actualDateStr = yesterday.toISOString().split('T')[0];

    httpMock.expectOne(`http://localhost:8080/dailyweather/cached?actualDate=${actualDateStr}`).flush([]);

    httpMock.expectOne('assets/austria-regions.geojson').flush({ type: 'FeatureCollection', features: [] });
    httpMock.expectOne('http://localhost:8080/mosquitoes').flush([]);
  }));
  
  

  it('should enable scroll zoom if ctrlKey is pressed', () => {
    const scrollZoomMock = {
      enable: jasmine.createSpy('enable'),
      disable: jasmine.createSpy('disable'),
    };

    const mockElement = document.createElement('div');
    spyOn(mockElement, 'addEventListener');
    spyOn(mockElement, 'removeEventListener');
    
    component.map = {
      scrollWheelZoom: scrollZoomMock,
      getMinZoom: () => 6,
      remove: jasmine.createSpy('remove'),
      on: jasmine.createSpy('on').and.callFake(function (this: L.Map) {
        return this;
      }),
      getContainer: () => document.createElement('div')
    } as unknown as L.Map;
  
    const event = new WheelEvent('wheel', { ctrlKey: true });
    component.handleMapScroll(event);
  
    expect(scrollZoomMock.enable).toHaveBeenCalled();
    expect(scrollZoomMock.disable).not.toHaveBeenCalled();
  });
  
  it('should call map.remove on destroy', () => {
    const removeSpy = jasmine.createSpy();
    component.map = { remove: removeSpy } as unknown as L.Map;
  
    component.ngOnDestroy();
  
    expect(removeSpy).toHaveBeenCalled();
  });
  
  it('should add markers for mosquito data', fakeAsync(() => {
    const markerMock = jasmine.createSpyObj<L.Marker>('marker', ['bindPopup', 'on', 'addTo', 'setIcon']);
    markerMock.bindPopup.and.returnValue(markerMock);
    markerMock.on.and.returnValue(markerMock);
  
    spyOn(L, 'marker').and.returnValue(markerMock);
  
    fixture.detectChanges();
  
    const yesterday = new Date();
    yesterday.setDate(yesterday.getDate() - 1);
    const actualDateStr = yesterday.toISOString().split('T')[0];

    httpMock.expectOne(`http://localhost:8080/dailyweather/cached?actualDate=${actualDateStr}`).flush([]);

    httpMock.expectOne('assets/austria-regions.geojson').flush({ type: 'FeatureCollection', features: [] });
  
    const mockData: MosquitoOccurrence[] = [{
      species: 'Aedes testus',
      eventDate: '2024-01-01',
      latitude: 48,
      longitude: 16
    }];
  
    httpMock.expectOne('http://localhost:8080/mosquitoes').flush(mockData);
    tick();
  
    expect(markerMock.addTo).toHaveBeenCalled();
  }));
  
  it('should handle marker click and popupclose', fakeAsync(() => {
    const mockOccurrence: MosquitoOccurrence = {
      species: 'Culex testicus',
      eventDate: '2024-05-01',
      latitude: 47,
      longitude: 16
    };
  
    const markerMock = jasmine.createSpyObj<L.Marker>('marker', [
      'bindPopup',
      'on',
      'addTo',
      'setIcon'
    ]);
  
    markerMock.bindPopup.and.returnValue(markerMock);
  
    let clickHandler: (() => void) | undefined;
    let popupCloseHandler: (() => void) | undefined;
  
    // TS workaround: strong typing vs real test needs – override
    markerMock.on.and.callFake(((event: string, cb: () => void) => {
      if (event === 'click') clickHandler = cb;
      if (event === 'popupclose') popupCloseHandler = cb;
      return markerMock;
    // eslint-disable-next-line @typescript-eslint/no-explicit-any
    }) as unknown as L.Marker<any>['on']);
  
    spyOn(L, 'marker').and.returnValue(markerMock);
    const selectionSpy = spyOn(component['selectionService'], 'setSelectedOccurrence');
  
    fixture.detectChanges();
  
    const yesterday = new Date();
    yesterday.setDate(yesterday.getDate() - 1);
    const actualDateStr = yesterday.toISOString().split('T')[0];

    httpMock.expectOne(`http://localhost:8080/dailyweather/cached?actualDate=${actualDateStr}`).flush([]);

    httpMock.expectOne('assets/austria-regions.geojson').flush({ type: 'FeatureCollection', features: [] });
    httpMock.expectOne('http://localhost:8080/mosquitoes').flush([mockOccurrence]);
    tick();
  
    if (clickHandler) {
      clickHandler();
    }
    if (popupCloseHandler) {
      popupCloseHandler();
    }
  
    expect(selectionSpy).toHaveBeenCalledTimes(2);
    expect(selectionSpy.calls.argsFor(0)[0]).toEqual(mockOccurrence);
    expect(selectionSpy.calls.argsFor(1)[0]).toBeNull();
  }));
  
  

  it('should set hasMosquitoError to true on mosquitoService error', fakeAsync(() => {
    spyOn(console, 'error');
  
    fixture.detectChanges();
  
    const yesterday = new Date();
    yesterday.setDate(yesterday.getDate() - 1);
    const actualDateStr = yesterday.toISOString().split('T')[0];

    httpMock.expectOne(`http://localhost:8080/dailyweather/cached?actualDate=${actualDateStr}`).flush([]);

    httpMock.expectOne('assets/austria-regions.geojson').flush({ type: 'FeatureCollection', features: [] });
    httpMock.expectOne('http://localhost:8080/mosquitoes').flush('Error', { status: 500, statusText: 'Server Error' });
  
    tick();
  
    expect(component.hasMosquitoError).toBeTrue();
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

  it('should register addEventListener and removeEventListener on focus and blur', fakeAsync(() => {
    const addEventListenerSpy = jasmine.createSpy('addEventListener');
    const removeEventListenerSpy = jasmine.createSpy('removeEventListener');

    const mockContainer = {
      addEventListener: addEventListenerSpy,
      removeEventListener: removeEventListenerSpy
    };

    const onSpy = jasmine.createSpy('on').and.callFake((event, callback) => {
      if (event === 'focus' || event === 'blur') callback();
      return fakeMap;
    });

    const fakeMap = {
    on: onSpy,
    getContainer: () => mockContainer,
    createPane: jasmine.createSpy(),
    getPane: jasmine.createSpy().and.returnValue(document.createElement('div')),
    scrollWheelZoom: {
      enable: jasmine.createSpy(),
      disable: jasmine.createSpy()
    },
    addLayer: jasmine.createSpy(),
    remove: jasmine.createSpy()
  } as unknown as L.Map;

    spyOn(L, 'map').and.returnValue(fakeMap);

    fixture.detectChanges();

    const yesterday = new Date();
    yesterday.setDate(yesterday.getDate() - 1);
    const actualDateStr = yesterday.toISOString().split('T')[0];

    httpMock.expectOne(`http://localhost:8080/dailyweather/cached?actualDate=${actualDateStr}`).flush([]);
    httpMock.expectOne('assets/austria-regions.geojson').flush({ type: 'FeatureCollection', features: [] });
    httpMock.expectOne('http://localhost:8080/mosquitoes').flush([]);

    tick();

    expect(addEventListenerSpy).toHaveBeenCalledWith('wheel', jasmine.any(Function));
    expect(removeEventListenerSpy).toHaveBeenCalledWith('wheel', jasmine.any(Function));
  }));

  it('should disable scroll zoom if ctrlKey is not pressed', () => {
    const scrollZoomMock = {
      enable: jasmine.createSpy(),
      disable: jasmine.createSpy()
    };

    component.map = {
      scrollWheelZoom: scrollZoomMock,
      getMinZoom: () => 6,
      remove: jasmine.createSpy('remove'),
      on: jasmine.createSpy('on'),
      getContainer: () => document.createElement('div')
    } as unknown as L.Map;

    const event = new WheelEvent('wheel', { ctrlKey: false });
    component.handleMapScroll(event);

    expect(scrollZoomMock.disable).toHaveBeenCalled();
    expect(scrollZoomMock.enable).not.toHaveBeenCalled();
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

    (component as unknown as { loadWeatherMarkers: () => void }).loadWeatherMarkers();

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

    expect(markerMock.addTo).toHaveBeenCalledWith(component['map']);
  }));
});
