import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';

import { environment } from 'src/environments/environment';
import {
  Disponibilite,
  DisponibiliteRequest,
  RendezVous,
  RendezVousRequest,
  Therapeute
} from '../models/rendez-vous.model';

@Injectable({ providedIn: 'root' })
export class RendezVousService {
  private http = inject(HttpClient);
  private readonly api = environment.apiUrl;

  // ===== Rendez-vous (patient, thérapeute, administrateur) =====

  /** Patient : les siens · Thérapeute : ceux qu'il reçoit · Admin : tous. */
  lister(): Observable<RendezVous[]> {
    return this.http.get<RendezVous[]>(`${this.api}/rendez-vous`);
  }

  prendre(req: RendezVousRequest): Observable<RendezVous> {
    return this.http.post<RendezVous>(`${this.api}/rendez-vous`, req);
  }

  confirmer(id: number): Observable<RendezVous> {
    return this.http.patch<RendezVous>(`${this.api}/rendez-vous/${id}/confirmer`, {});
  }

  annuler(id: number): Observable<RendezVous> {
    return this.http.patch<RendezVous>(`${this.api}/rendez-vous/${id}/annuler`, {});
  }

  // ===== Choix d'un thérapeute (patient) =====

  therapeutes(): Observable<Therapeute[]> {
    return this.http.get<Therapeute[]>(`${this.api}/therapeutes`);
  }

  creneauxLibres(therapeuteId: number): Observable<Disponibilite[]> {
    return this.http.get<Disponibilite[]>(`${this.api}/therapeutes/${therapeuteId}/disponibilites`);
  }

  // ===== Disponibilités (thérapeute) =====

  mesDisponibilites(): Observable<Disponibilite[]> {
    return this.http.get<Disponibilite[]>(`${this.api}/disponibilites`);
  }

  ajouterDisponibilite(req: DisponibiliteRequest): Observable<Disponibilite> {
    return this.http.post<Disponibilite>(`${this.api}/disponibilites`, req);
  }

  supprimerDisponibilite(id: number): Observable<void> {
    return this.http.delete<void>(`${this.api}/disponibilites/${id}`);
  }
}
