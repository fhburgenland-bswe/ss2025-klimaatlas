import { Injectable } from '@angular/core';
import { BehaviorSubject } from 'rxjs';
import { MosquitoOccurrence } from '../interfaces/mosquito-occurrence.interface';
@Injectable({
  providedIn: 'root'
})
export class SelectionService {
  private selectedOccurrenceSubject = new BehaviorSubject<MosquitoOccurrence | null>(null);
  selectedOccurrence$ = this.selectedOccurrenceSubject.asObservable();

  setSelectedOccurrence(occurrence: MosquitoOccurrence | null) {
    this.selectedOccurrenceSubject.next(occurrence);
  }
}
