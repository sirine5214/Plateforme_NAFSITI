import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';

import { environment } from 'src/environments/environment';
import { Contact, EnvoiMessage, Message, MessageModeration } from '../models/messagerie.model';

/** Module 5 — messagerie : envoi par REST (modéré par l'IA), réception via TempsReelService. */
@Injectable({ providedIn: 'root' })
export class MessagerieService {
  private http = inject(HttpClient);
  private readonly api = environment.apiUrl;

  contacts(): Observable<Contact[]> {
    return this.http.get<Contact[]>(`${this.api}/messages/contacts`);
  }

  conversation(autreId: number): Observable<Message[]> {
    return this.http.get<Message[]>(`${this.api}/messages/${autreId}`);
  }

  envoyer(destinataireId: number, contenu: string): Observable<EnvoiMessage> {
    return this.http.post<EnvoiMessage>(`${this.api}/messages`, { destinataireId, contenu });
  }

  // ===== Revue humaine (administrateur) =====

  enAttente(): Observable<MessageModeration[]> {
    return this.http.get<MessageModeration[]>(`${this.api}/moderation/messages`);
  }

  decider(id: number, publier: boolean): Observable<void> {
    return this.http.patch<void>(`${this.api}/moderation/messages/${id}`, { publier });
  }
}
