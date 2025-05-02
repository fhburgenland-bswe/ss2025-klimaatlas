import { ComponentFixture, TestBed, fakeAsync, tick } from '@angular/core/testing';
import { MapComponent } from './map.component';
import { HttpClientTestingModule, HttpTestingController } from '@angular/common/http/testing';
import { MapService } from '../../services/map.service';
import * as L from 'leaflet';

describe('MapComponent', () => {
  let component: MapComponent;
  let fixture: ComponentFixture<MapComponent>;
  let httpMock: HttpTestingController;
  let mapService: MapService;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [MapComponent, HttpClientTestingModule],
      providers: [
        {
          provide: MapService,
          useValue: {
            setMap: jasmine.createSpy('setMap')
          }
        }
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
});
