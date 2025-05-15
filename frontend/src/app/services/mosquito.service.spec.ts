import { TestBed } from '@angular/core/testing';
import { MosquitoService } from './mosquito.service';
import { HttpClientTestingModule, HttpTestingController } from '@angular/common/http/testing';
import { MosquitoOccurrence } from '../interfaces/mosquito-occurrence.interface';


describe('MosquitoService', () => {
  let service: MosquitoService;
  let httpMock: HttpTestingController;
  
  beforeEach(() => {
    TestBed.configureTestingModule({
      imports: [HttpClientTestingModule]
    });
    service = TestBed.inject(MosquitoService);
    httpMock = TestBed.inject(HttpTestingController);
  });

  afterEach(() => {
    httpMock.verify();
  });

  it('should be created', () => {
    expect(service).toBeTruthy();
  });

  it('should fetch mosquito occurrences', () => {
    const mockData: MosquitoOccurrence[] = [
      {
        species: 'Aedes albopictus',
        eventDate: '2024-05-15',
        latitude: 47.0707,
        longitude: 15.4395
      }
    ];

    service.getMosquitoOccurrences().subscribe(data => {
      expect(data).toEqual(mockData);
    });

    const req = httpMock.expectOne('http://localhost:8080/mosquitoes');
    expect(req.request.method).toBe('GET');
    req.flush(mockData);
  });
});
