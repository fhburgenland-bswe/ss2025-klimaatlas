import { ComponentFixture, TestBed, fakeAsync, tick } from '@angular/core/testing';
import { ContentComponent } from './content.component';
import { HttpClientTestingModule } from '@angular/common/http/testing';
import { MapService } from '../../../../services/map.service';
import { WeatherService } from '../../../../services/weather.service';
import { of } from 'rxjs';
import { City } from '../../../../interfaces/city-interface';
import * as L from 'leaflet';

describe('ContentComponent', () => {
  let component: ContentComponent;
  let fixture: ComponentFixture<ContentComponent>;

  let mockMapService: Partial<MapService>;
  let mockWeatherService: Partial<WeatherService>;

  beforeEach(async () => {

    mockMapService = {
      getCities: jasmine.createSpy().and.returnValue(of([{ place: 'TestCity', zipcode: '12345', latitude: '47.0', longitude: '19.0' }])),
      getMap: jasmine.createSpy().and.returnValue(L.map(document.createElement('div'))),
      loadDistricts: jasmine.createSpy()
    };

    mockWeatherService = {
      getCachedWeatherReports: jasmine.createSpy().and.returnValue(of([])),
      getWeatherReportByCoords: jasmine.createSpy().and.returnValue(of({
        cityName: 'TestCity',
        minTemp: 10,
        maxTemp: 20,
        precip: 1
      }))
    };

    await TestBed.configureTestingModule({
      imports: [HttpClientTestingModule, ContentComponent],
      providers: [
        { provide: MapService, useValue: mockMapService },
        { provide: WeatherService, useValue: mockWeatherService },
      ]
    }).compileComponents();

    fixture = TestBed.createComponent(ContentComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });

  it('should load cities on init', () => {
    expect(mockMapService.getCities).toHaveBeenCalled();
    expect(component.cities.length).toBeGreaterThan(0);
  });

  it('should toggle panel state', () => {
    const initial = component.isCollapsed;
    component.togglePanel();
    expect(component.isCollapsed).toBe(!initial);
  });

  it('should filter cities by search term', () => {
    component.cities = [
      {
        place: 'Wien', zipcode: '1000', latitude: '47.0', longitude: '19.0',
        country_code: '',
        state: '',
        state_code: '',
        province: '',
        province_code: '',
        community: '',
        community_code: ''
      },
      {
        place: 'Linz', zipcode: '2000', latitude: '47.5', longitude: '21.6',
        country_code: '',
        state: '',
        state_code: '',
        province: '',
        province_code: '',
        community: '',
        community_code: ''
      }
    ];
    component.searchTerm = 'wie';
    component.onSearchChange();
    expect(component.filteredCities.length).toBe(1);
    expect(component.filteredCities[0].place).toBe('Wien');
  });

  it('should select a place and fetch weather data', fakeAsync(() => {
    const testCity: City = {
      place: 'TestCity',
      zipcode: '12345',
      latitude: '47.0',
      longitude: '19.0',
      country_code: '',
      state: '',
      state_code: '',
      province: '',
      province_code: '',
      community: '',
      community_code: ''
    };
    component.selectPlace(testCity);
    tick();
    expect(mockWeatherService.getCachedWeatherReports).toHaveBeenCalled();
    expect(mockMapService.loadDistricts).toHaveBeenCalledWith(testCity);
  }));
});