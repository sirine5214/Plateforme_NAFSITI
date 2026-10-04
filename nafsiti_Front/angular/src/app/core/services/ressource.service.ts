import { Injectable, inject } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable } from 'rxjs';

import { environment } from 'src/environments/environment';
import { Recommandations, Ressource, RessourceRequest } from '../models/ressource.model';

/** Module 4 — bibliothèque de ressources et recommandations IA. */
@Injectable({ providedIn: 'root' })
export class RessourceService {
  private http = inject(HttpClient);
  private readonly api = `${environment.apiUrl}/ressources`;

  lister(): Observable<Ressource[]> {
    return this.http.get<Ressource[]>(this.api);
  }

  recommandations(): Observable<Recommandations> {
    return this.http.get<Recommandations>(`${this.api}/recommandations`);
  }

  consulter(id: number): Observable<Ressource> {
    return this.http.post<Ressource>(`${this.api}/${id}/consulter`, {});
  }

  aimer(id: number, valeur: boolean): Observable<Ressource> {
    return this.http.put<Ressource>(`${this.api}/${id}/aime`, {}, { params: new HttpParams().set('valeur', valeur) });
  }

  // ===== Administration =====

  creer(req: RessourceRequest): Observable<Ressource> {
    return this.http.post<Ressource>(this.api, req);
  }

  modifier(id: number, req: RessourceRequest): Observable<Ressource> {
    return this.http.put<Ressource>(`${this.api}/${id}`, req);
  }

  supprimer(id: number): Observable<void> {
    return this.http.delete<void>(`${this.api}/${id}`);
  }
}
