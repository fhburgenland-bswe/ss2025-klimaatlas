import { ComponentFixture, fakeAsync, TestBed, tick, waitForAsync } from '@angular/core/testing';
import { ContentComponent } from './content.component';
import { MapService } from '../../../../services/map.service';
import { City } from '../../../../interfaces/city-interface';
import * as L from 'leaflet';
import { BehaviorSubject, Observable, of, throwError } from 'rxjs';
import { WeatherService } from '../../../../services/weather.service';
import { SelectionService } from '../../../../services/selection.service';
import { translatePrecipitation } from '../../../../utils/precipitation-translator';
import { WeatherReportDTO } from '../../../../interfaces/weather';

describe('ContentComponent', () => {
  let component: ContentComponent;
  let fixture: ComponentFixture<ContentComponent>;
  let mapService: jasmine.SpyObj<MapService>;
  let fakeMap: jasmine.SpyObj<L.Map>;
  let httpGetSpy: jasmine.Spy;
  let weatherService: jasmine.SpyObj<WeatherService>;
  let selectionService: jasmine.SpyObj<SelectionService>;
  let selectedWeatherReportSubject: BehaviorSubject<WeatherReportDTO | null>;

  const mockCities: City[] = [
    {
      place: 'Wien', zipcode: '1010', latitude: '48.2082', longitude: '16.3738',
      country_code: '', state: '', state_code: '', province: '',
      province_code: '', community: '', community_code: 'AT-9'
    },
    {
      place: 'Graz', zipcode: '8010', latitude: '47.0707', longitude: '15.4395',
      country_code: '', state: '', state_code: '', province: '',
      province_code: '', community: '', community_code: 'AT-6'
    }
  ];

  beforeEach(async () => {
    selectedWeatherReportSubject = new BehaviorSubject<WeatherReportDTO | null>(null);
    weatherService = jasmine.createSpyObj('WeatherService', ['getCachedWeatherReports']);
    selectionService = jasmine.createSpyObj('SelectionService', ['setSelectedWeatherReport'], {
      selectedWeatherReport$: selectedWeatherReportSubject.asObservable()
    });
    const mapSpy = jasmine.createSpyObj<L.Map>('map', ['setView', 'removeLayer', 'addLayer']);
    const mapServiceSpy = jasmine.createSpyObj<MapService>('MapService', ['getMap', 'getCities', 'loadDistricts']);
    weatherService.getCachedWeatherReports.and.returnValue(of([]));

    await TestBed.configureTestingModule({
      imports: [ContentComponent],
      providers: [
        { provide: MapService, useValue: mapServiceSpy },
        { provide: SelectionService, useValue: selectionService },
        { provide: WeatherService, useValue: weatherService }
      ]
    }).compileComponents();

    fixture = TestBed.createComponent(ContentComponent);
    component = fixture.componentInstance;
    mapService = TestBed.inject(MapService) as jasmine.SpyObj<MapService>;
    fakeMap = mapSpy;

    // eslint-disable-next-line
    httpGetSpy = spyOn(component['http'], 'get').and.callFake((url: string): Observable<any> => {
      if (url.includes('load-status.php')) {
        return of('Mock Health Status Content');
      } else if (url.includes('load-risk.php')) {
        return of('Mock Health Risk Content');
      }
      return of(null);
    });

    mapService.getMap.and.returnValue(fakeMap);
    mapService.getCities.and.returnValue(of(mockCities));

    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });

  describe('onSearchChange', () => {
    it('should clear filteredCities if searchTerm is less than 2 characters', () => {
      component.searchTerm = 'w';
      component.onSearchChange();
      expect(component.filteredCities.length).toBe(0);
    });

    it('should filter cities by place or zipcode', () => {
      component.searchTerm = 'wi';
      component.onSearchChange();
      expect(component.filteredCities[0].place).toBe('Wien');

      component.searchTerm = '8010';
      component.onSearchChange();
      expect(component.filteredCities[0].place).toBe('Graz');
    });
  });

  describe('onEnterKey', () => {
    it('should select matched city by place or zipcode', () => {
      const selectSpy = spyOn(component, 'selectPlace');
      component.searchTerm = 'Graz';
      const event = new KeyboardEvent('keydown', { key: 'Enter' });
      component.onEnterKey(event);
      expect(selectSpy).toHaveBeenCalledWith(mockCities[1]);

      component.searchTerm = '1010';
      component.onEnterKey(event);
      expect(selectSpy).toHaveBeenCalledWith(mockCities[0]);
    });

    it('should not call selectPlace if no match', () => {
      const selectSpy = spyOn(component, 'selectPlace');
      component.searchTerm = 'unknown';
      component.onEnterKey(new KeyboardEvent('keydown', { key: 'Enter' }));
      expect(selectSpy).not.toHaveBeenCalled();
    });
  });

  describe('selectPlace', () => {
    beforeEach(() => {
      const fakePopup = { openPopup: jasmine.createSpy() };
      const fakeLayer = { bindPopup: () => fakePopup };
      const fakeMarker = {
        addTo: jasmine.createSpy().and.returnValue(fakeLayer),
      };
      spyOn(L, 'marker').and.returnValue(fakeMarker as unknown as L.Marker);
    });

    it('should set view, add marker, and call loadDistricts', () => {
      component.selectPlace(mockCities[0]);

      expect(fakeMap.setView)
        .toHaveBeenCalledWith([parseFloat(mockCities[0].latitude), parseFloat(mockCities[0].longitude)], 15);

      expect(mapService.loadDistricts).toHaveBeenCalledWith(mockCities[0]);
      expect(component.searchTerm).toContain('Wien');
      expect(component.filteredCities.length).toBe(0);
    });

    it('should remove previous marker if it exists', () => {
      const marker = {} as L.Marker;
      component.selectedMarker = marker;
      component.selectPlace(mockCities[1]);
      expect(fakeMap.removeLayer).toHaveBeenCalledWith(marker);
    });

    it('should set fallback message and log error when health status request fails', waitForAsync(() => {
      const consoleSpy = spyOn(console, 'error');

      httpGetSpy.and.callFake((url: string) => {
        if (url === 'http://localhost:8081/load-status.php') {
          return throwError(() => new ErrorEvent('Network error'));
        }
        return of();
      });

      component.loadHealthStatusData();

      fixture.whenStable().then(() => {
        expect(consoleSpy).toHaveBeenCalledWith(
          'Failure when loading the Health Status content',
          jasmine.any(ErrorEvent)
        );
        expect(component.healthStatusContent).toBe('Health Status Content is not available at the moment.');
      });
    }));

    it('should set fallback message and log error when health risk request fails', waitForAsync(() => {
      const consoleSpy = spyOn(console, 'error');

      httpGetSpy.and.callFake((url: string) => {
        if (url === 'http://localhost:8081/load-risk.php') {
          return throwError(() => new ErrorEvent('Network error'));
        }
        return of();
      });

      component.loadHealthRiskData();

      fixture.whenStable().then(() => {
        expect(consoleSpy).toHaveBeenCalledWith(
          'Failure when loading the Health Risk content',
          jasmine.any(ErrorEvent)
        );
        expect(component.healthRiskContent).toBe('Health Risk Content is not available at the moment.');
      });
    }));

    it('should toggle isCollapsed value', () => {
      component.isCollapsed = false;

      component.togglePanel();
      expect(component.isCollapsed).toBeTrue();

      component.togglePanel();
      expect(component.isCollapsed).toBeFalse();
    });
  });

  it('should update weatherInfo when selectedWeatherReport$ emits a report', fakeAsync(() => {
    const mockedReport: WeatherReportDTO = {
      cityName: 'Budapest',
      minTemp: 10,
      maxTemp: 20,
      precip: 'RAIN',
      sunDuration: null,
      latitude: 47.4979,
      longitude: 19.0402
    };

    component.ngOnInit();
    tick();

    selectedWeatherReportSubject.next(mockedReport);

    tick();
    fixture.detectChanges();

    expect(component.weatherInfo).toEqual({
      cityName: 'Budapest',
      minTemp: 10,
      maxTemp: 20,
      precipitation: translatePrecipitation('RAIN'),
    });
  }));

  it('should set weatherInfo to null when selectedWeatherReport$ emits null', fakeAsync(() => {
    component.ngOnInit();
    tick();

    selectedWeatherReportSubject.next(null);
    tick();
    fixture.detectChanges();

    expect(component.weatherInfo).toBeNull();
  }));

  it('should set weatherInfo and call setSelectedWeatherReport with the first report if reports are available', fakeAsync(() => {
    const mockReports: WeatherReportDTO[] = [
      {
        cityName: 'Budapest',
        minTemp: 5,
        maxTemp: 15,
        precip: 'RAIN',
        sunDuration: null,
        latitude: 47.5,
        longitude: 19.0
      }
    ];

    weatherService.getCachedWeatherReports.and.returnValue(of(mockReports));

    component.loadTemperatureData();

    tick();
    fixture.detectChanges();

    expect(selectionService.setSelectedWeatherReport).toHaveBeenCalledWith(mockReports[0]);
    expect(component.weatherInfo).toEqual({
      cityName: 'Budapest',
      minTemp: 5,
      maxTemp: 15,
      precipitation: translatePrecipitation('RAIN')
    });
  }));

  it('should set weatherInfo to null and call setSelectedWeatherReport with null if no reports', fakeAsync(() => {
    weatherService.getCachedWeatherReports.and.returnValue(of([]));

    component.loadTemperatureData();

    tick();
    fixture.detectChanges();

    expect(selectionService.setSelectedWeatherReport).toHaveBeenCalledWith(null);
    expect(component.weatherInfo).toBeNull();
  }));

  it('should set weatherInfo to null and call setSelectedWeatherReport with null on error', fakeAsync(() => {
    weatherService.getCachedWeatherReports.and.returnValue(throwError(() => new Error('Network error')));

    component.loadTemperatureData();

    tick();
    fixture.detectChanges();

    expect(selectionService.setSelectedWeatherReport).toHaveBeenCalledWith(null);
    expect(component.weatherInfo).toBeNull();
  }));

  it('should set selected weather report if match found', () => {
    const mockCity = {
      place: 'Budapest',
      zipcode: '1010',
      latitude: '47.4979',
      longitude: '19.0402',
      country_code: '', state: '', state_code: '', province: '',
      province_code: '', community: '', community_code: ''
    };
    const mockReport: WeatherReportDTO = {
      cityName: 'Budapest',
      minTemp: 10,
      maxTemp: 20,
      precip: 'NONE',
      sunDuration: null,
      latitude: 0,
      longitude: 0
    };

    weatherService.getCachedWeatherReports.and.returnValue(of([mockReport]));

    component.selectPlace(mockCity);

    expect(selectionService.setSelectedWeatherReport).toHaveBeenCalledWith(mockReport);
    expect(component.weatherInfo).toEqual({
      cityName: 'Budapest',
      minTemp: 10,
      maxTemp: 20,
      precipitation: translatePrecipitation('rain')
    });
  });

  it('should set selected weather report to null if no match found', () => {
    const mockCity = {
      place: 'Budapest',
      zipcode: '1010',
      latitude: '47.4979',
      longitude: '19.0402',
      country_code: '', state: '', state_code: '', province: '',
      province_code: '', community: '', community_code: ''
    };
    const mockReport: WeatherReportDTO = {
      cityName: 'Szeged',
      minTemp: 5,
      maxTemp: 15,
      precip: 'RAIN',
      sunDuration: null,
      latitude: 0,
      longitude: 0
    };

    weatherService.getCachedWeatherReports.and.returnValue(of([mockReport]));

    component.selectPlace(mockCity);

    expect(selectionService.setSelectedWeatherReport).toHaveBeenCalledWith(null);
    expect(component.weatherInfo).toBeNull();
  });

  it('should set selected weather report to null on error', () => {
    const mockCity = {
      place: 'Budapest',
      zipcode: '1010',
      latitude: '47.4979',
      longitude: '19.0402',
      country_code: '', state: '', state_code: '', province: '',
      province_code: '', community: '', community_code: ''
    };
    
    weatherService.getCachedWeatherReports.and.returnValue(throwError(() => new Error('Error')));

    component.selectPlace(mockCity);

    expect(selectionService.setSelectedWeatherReport).toHaveBeenCalledWith(null);
    expect(component.weatherInfo).toBeNull();
  });
});