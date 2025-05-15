import { HttpClient } from '@angular/common/http';
import { Injectable } from '@angular/core';
import { Observable } from 'rxjs';
import { MosquitoOccurrence } from '../interfaces/mosquito-occurrence.interface';

@Injectable({
  providedIn: 'root'
})
export class MosquitoService {

  private apiUrl = 'http://localhost:8080/mosquitoes';

  constructor(private http: HttpClient) {}

  getMosquitoOccurrences(): Observable<MosquitoOccurrence[]> {
    return this.http.get<MosquitoOccurrence[]>(this.apiUrl);
  }
}
