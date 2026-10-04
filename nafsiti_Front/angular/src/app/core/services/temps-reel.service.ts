import { Injectable, effect, inject, signal } from '@angular/core';
import { Observable, Subject, filter, map } from 'rxjs';

import { environment } from 'src/environments/environment';
import { EvenementTempsReel } from '../models/messagerie.model';
import { AuthService } from './auth.service';

/**
 * WebSocket /ws du backend : messages et alertes poussés en temps réel.
 * Le JWT est envoyé dans le premier message (pas dans l'URL). Reconnexion automatique.
 */
@Injectable({ providedIn: 'root' })
export class TempsReelService {
  private auth = inject(AuthService);

  private ws: WebSocket | null = null;
  private tentatives = 0;
  private timerReconnexion?: ReturnType<typeof setTimeout>;
  private readonly evenements = new Subject<EvenementTempsReel>();

  readonly connecte = signal(false);

  constructor() {
    // Fermeture à la déconnexion de l'utilisateur
    effect(() => {
      if (!this.auth.estConnecte()) {
        this.fermer();
      }
    });
  }

  /** Événements d'un type donné ; ouvre la connexion si nécessaire. */
  ecouter<T>(type: EvenementTempsReel['type']): Observable<T> {
    this.ouvrir();
    return this.evenements.pipe(
      filter((e) => e.type === type),
      map((e) => e.donnees as T)
    );
  }

  private ouvrir() {
    const token = this.auth.token;
    if (this.ws || !token || !this.auth.estConnecte()) return;
    const ws = new WebSocket(environment.wsUrl);
    this.ws = ws;
    ws.onopen = () => ws.send(JSON.stringify({ type: 'auth', token }));
    ws.onmessage = (m) => {
      try {
        const evenement = JSON.parse(m.data) as EvenementTempsReel;
        if (evenement.type === 'PRET') {
          this.connecte.set(true);
          this.tentatives = 0;
        }
        this.evenements.next(evenement);
      } catch {
        // message illisible ignoré
      }
    };
    ws.onclose = () => {
      this.ws = null;
      this.connecte.set(false);
      if (this.auth.estConnecte()) {
        const delai = Math.min(30_000, 1000 * 2 ** this.tentatives++);
        this.timerReconnexion = setTimeout(() => this.ouvrir(), delai);
      }
    };
  }

  private fermer() {
    clearTimeout(this.timerReconnexion);
    this.tentatives = 0;
    if (this.ws) {
      const ws = this.ws;
      this.ws = null;
      ws.onclose = null;
      ws.close();
    }
    this.connecte.set(false);
  }
}
