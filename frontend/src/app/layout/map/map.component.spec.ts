import { ComponentFixture, TestBed, fakeAsync, tick } from '@angular/core/testing';
import { MapComponent } from './map.component';
import { HttpClientTestingModule, HttpTestingController } from '@angular/common/http/testing';
import { MapService } from '../../services/map.service';
import * as L from 'leaflet';
import { MosquitoService } from '../../services/mosquito.service';
import { MosquitoOccurrence } from '../../interfaces/mosquito-occurrence.interface';

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
        MosquitoService
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

    const mosquitoReq = httpMock.expectOne('http://localhost:8080/mosquitoes');
    mosquitoReq.flush([]);

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
      addLayer: jasmine.createSpy('addLayer')
    } as unknown as L.Map;
  
    spyOn(L, 'map').and.returnValue(fakeMap);
  
    fixture.detectChanges();
    tick();
  
    expect(onSpy).toHaveBeenCalledWith('focus', jasmine.any(Function));
    expect(onSpy).toHaveBeenCalledWith('blur', jasmine.any(Function));
  
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
  
  

  it('should set hasError to true on mosquitoService error', fakeAsync(() => {
    spyOn(console, 'error');
  
    fixture.detectChanges();
  
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
  
  
});
