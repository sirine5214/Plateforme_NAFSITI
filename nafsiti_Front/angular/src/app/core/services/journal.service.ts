import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';

import { environment } from 'src/environments/environment';
import { EntreeJournal, JournalCreation, JournalRequest, Tendance } from '../models/journal.model';

/** Module 3 — journal d'humeur (patient). */
@Injectable({ providedIn: 'root' })
export class JournalService {
  private http = inject(HttpClient);
  private readonly api = `${environment.apiUrl}/journal`;

  lister(): Observable<EntreeJournal[]> {
    return this.http.get<EntreeJournal[]>(this.api);
  }

  creer(req: JournalRequest): Observable<JournalCreation> {
    return this.http.post<JournalCreation>(this.api, req);
  }

  supprimer(id: number): Observable<void> {
    return this.http.delete<void>(`${this.api}/${id}`);
  }

  tendance(): Observable<Tendance> {
    return this.http.get<Tendance>(`${this.api}/tendance`);
  }
}
