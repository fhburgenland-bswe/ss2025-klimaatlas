import { TestBed } from '@angular/core/testing';
import { WeatherService } from './weather.service';
import { HttpClientTestingModule, HttpTestingController } from '@angular/common/http/testing';
import { WeatherReportDTO } from '../interfaces/weather';

describe('WeatherService', () => {
  let service: WeatherService;
  let httpMock: HttpTestingController;

  beforeEach(() => {
    TestBed.configureTestingModule({
      imports: [HttpClientTestingModule],
      providers: [WeatherService]
    });
    service = TestBed.inject(WeatherService);
    httpMock = TestBed.inject(HttpTestingController);
  });

  afterEach(() => {
    httpMock.verify();
  });

  it('should be created', () => {
    expect(service).toBeTruthy();
  });

  it('should fetch cached weather reports for yesterday', () => {
    const mockData: WeatherReportDTO[] = [
      {
        minTemp: 10,
        maxTemp: 22,
        precip: 'RAIN',
        sunDuration: 180,
        latitude: 48.2082,
        longitude: 16.3738,
        cityName: 'Vienna'
      }
    ];

    const yesterday = new Date();
    yesterday.setDate(yesterday.getDate() - 1);
    const expectedDateStr = yesterday.toISOString().split('T')[0];
    const expectedUrl = `http://localhost:8080/dailyweather/cached?actualDate=${expectedDateStr}`;

    service.getCachedWeatherReports().subscribe(data => {
      expect(data).toEqual(mockData);
    });

    const req = httpMock.expectOne(expectedUrl);
    expect(req.request.method).toBe('GET');
    req.flush(mockData);
  });
});