import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';

import { environment } from 'src/environments/environment';
import { Alerte } from '../models/alerte.model';

/** Module 6 — alertes de détresse (thérapeute, administrateur). */
@Injectable({ providedIn: 'root' })
export class AlerteService {
  private http = inject(HttpClient);
  private readonly api = `${environment.apiUrl}/alertes`;

  lister(): Observable<Alerte[]> {
    return this.http.get<Alerte[]>(this.api);
  }

  traiter(id: number): Observable<Alerte> {
    return this.http.patch<Alerte>(`${this.api}/${id}/traiter`, {});
  }
}
