import { Injectable, inject } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable } from 'rxjs';

import { environment } from 'src/environments/environment';
import {
  ChangementMotDePasseRequest,
  ProfilRequest,
  Utilisateur,
  UtilisateurFiltres,
  UtilisateurRequest,
  UtilisateurStats
} from '../models/utilisateur.model';

@Injectable({ providedIn: 'root' })
export class UtilisateurService {
  private http = inject(HttpClient);
  private readonly api = `${environment.apiUrl}/utilisateurs`;

  // ===== Administration =====

  lister(filtres: UtilisateurFiltres = {}): Observable<Utilisateur[]> {
    let params = new HttpParams();
    if (filtres.recherche?.trim()) params = params.set('recherche', filtres.recherche.trim());
    if (filtres.role) params = params.set('role', filtres.role);
    if (filtres.actif !== undefined && filtres.actif !== '') params = params.set('actif', String(filtres.actif));
    return this.http.get<Utilisateur[]>(this.api, { params });
  }

  statistiques(): Observable<UtilisateurStats> {
    return this.http.get<UtilisateurStats>(`${this.api}/stats`);
  }

  creer(req: UtilisateurRequest): Observable<Utilisateur> {
    return this.http.post<Utilisateur>(this.api, req);
  }

  modifier(id: number, req: UtilisateurRequest): Observable<Utilisateur> {
    return this.http.put<Utilisateur>(`${this.api}/${id}`, req);
  }

  changerStatut(id: number, actif: boolean): Observable<Utilisateur> {
    return this.http.patch<Utilisateur>(`${this.api}/${id}/statut`, { actif });
  }

  supprimer(id: number): Observable<void> {
    return this.http.delete<void>(`${this.api}/${id}`);
  }

  // ===== Profil connecté =====

  modifierProfil(req: ProfilRequest): Observable<Utilisateur> {
    return this.http.put<Utilisateur>(`${this.api}/me`, req);
  }

  changerMotDePasse(req: ChangementMotDePasseRequest): Observable<void> {
    return this.http.put<void>(`${this.api}/me/mot-de-passe`, req);
  }
}
