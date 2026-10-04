import { Injectable, computed, inject, signal } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Router } from '@angular/router';
import { Observable, tap } from 'rxjs';

import { environment } from 'src/environments/environment';
import { AuthResponse, LoginRequest, LoginVisageRequest, RegisterRequest, Role, Utilisateur } from '../models/utilisateur.model';

const TOKEN_KEY = 'nafsiti_token';
const USER_KEY = 'nafsiti_user';

@Injectable({ providedIn: 'root' })
export class AuthService {
  private http = inject(HttpClient);
  private router = inject(Router);
  private readonly api = `${environment.apiUrl}/auth`;

  private readonly _token = signal<string | null>(localStorage.getItem(TOKEN_KEY));
  private readonly _utilisateur = signal<Utilisateur | null>(this.lireUtilisateur());

  readonly utilisateur = this._utilisateur.asReadonly();
  readonly estConnecte = computed(() => !!this._token() && !!this._utilisateur() && !this.tokenExpire(this._token()));
  readonly estAdmin = computed(() => this._utilisateur()?.role === 'ADMINISTRATEUR');

  get token(): string | null {
    return this._token();
  }

  /** Connexion manuelle. Si `mfaRequis`, la session n'est pas ouverte : appeler `validerMfaVisage`. */
  login(req: LoginRequest): Observable<AuthResponse> {
    return this.http.post<AuthResponse>(`${this.api}/login`, req).pipe(tap((r) => this.ouvrirSession(r)));
  }

  /** Connexion par reconnaissance faciale (email + capture webcam). */
  loginVisage(req: LoginVisageRequest): Observable<AuthResponse> {
    return this.http.post<AuthResponse>(`${this.api}/login/visage`, req).pipe(tap((r) => this.ouvrirSession(r)));
  }

  /** Second facteur demandé par l'IA (connexion inhabituelle) : confirmation par le visage. */
  validerMfaVisage(mfaToken: string, image: string): Observable<AuthResponse> {
    return this.http
      .post<AuthResponse>(`${this.api}/mfa/visage`, { mfaToken, image })
      .pipe(tap((r) => this.ouvrirSession(r)));
  }

  register(req: RegisterRequest): Observable<AuthResponse> {
    return this.http.post<AuthResponse>(`${this.api}/register`, req).pipe(tap((r) => this.ouvrirSession(r)));
  }

  /** Recharge le profil depuis le backend (rôle/statut à jour). */
  rafraichir(): Observable<Utilisateur> {
    return this.http.get<Utilisateur>(`${this.api}/me`).pipe(tap((u) => this.majUtilisateur(u)));
  }

  majUtilisateur(u: Utilisateur) {
    localStorage.setItem(USER_KEY, JSON.stringify(u));
    this._utilisateur.set(u);
  }

  aUnRole(roles: Role[]): boolean {
    const role = this._utilisateur()?.role;
    return !!role && roles.includes(role);
  }

  logout(redirect = true) {
    // Révoque le JWT côté backend (best effort : la session locale est fermée quoi qu'il arrive).
    if (this._token()) {
      this.http.post<void>(`${this.api}/logout`, {}).subscribe({ error: () => undefined });
    }
    localStorage.removeItem(TOKEN_KEY);
    localStorage.removeItem(USER_KEY);
    this._token.set(null);
    this._utilisateur.set(null);
    if (redirect) {
      this.router.navigate(['/login']);
    }
  }

  private ouvrirSession(r: AuthResponse) {
    if (r.mfaRequis || !r.token || !r.utilisateur) {
      return; // vérification faciale encore nécessaire
    }
    localStorage.setItem(TOKEN_KEY, r.token);
    this._token.set(r.token);
    this.majUtilisateur(r.utilisateur);
  }

  private lireUtilisateur(): Utilisateur | null {
    try {
      const raw = localStorage.getItem(USER_KEY);
      return raw ? (JSON.parse(raw) as Utilisateur) : null;
    } catch {
      return null;
    }
  }

  private tokenExpire(token: string | null): boolean {
    if (!token) return true;
    try {
      const payload = JSON.parse(atob(token.split('.')[1].replace(/-/g, '+').replace(/_/g, '/')));
      return typeof payload.exp === 'number' && payload.exp * 1000 < Date.now();
    } catch {
      return true;
    }
  }
}
