import { TestBed } from '@angular/core/testing';

import { SelectionService } from './selection.service';
import { MosquitoOccurrence } from '../interfaces/mosquito-occurrence.interface';

import { skip } from 'rxjs/operators';

describe('SelectionService', () => {
  let service: SelectionService;

  beforeEach(() => {
    TestBed.configureTestingModule({});
    service = TestBed.inject(SelectionService);
  });

  it('should be created', () => {
    expect(service).toBeTruthy();
  });

  it('should set and emit selected occurrence', (done) => {
    const mockOccurrence: MosquitoOccurrence = {
      species: 'Aedes japonicus',
      eventDate: '2024-06-01',
      latitude: 48.2082,
      longitude: 16.3738
    };
  
    service.selectedOccurrence$.pipe(skip(1)).subscribe(value => {
      expect(value).toEqual(mockOccurrence);
      done();
    });
  
    service.setSelectedOccurrence(mockOccurrence);
  });

  it('should emit null when clearing selection', (done) => {
    service.setSelectedOccurrence({
      species: 'dummy',
      eventDate: '2020-01-01',
      latitude: 0,
      longitude: 0
    });
  
    service.selectedOccurrence$.pipe(skip(1)).subscribe(value => {
      expect(value).toBeNull();
      done();
    });
  
    service.setSelectedOccurrence(null);
  });
});
