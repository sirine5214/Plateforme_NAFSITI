import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';

import { environment } from 'src/environments/environment';

export interface ActionChatbot {
  libelle: string;
  /** Route de l'application (/ressources…) ou numéro (tel:3114) */
  lien: string;
}

export interface ReponseChatbot {
  reponse: string;
  actions: ActionChatbot[];
  /** 0-3 : à partir de 2, les numéros d'urgence sont mis en avant */
  niveauRisque: number;
}

/** Chatbot d'orientation (aucune conversation n'est enregistrée côté serveur). */
@Injectable({ providedIn: 'root' })
export class ChatbotService {
  private http = inject(HttpClient);

  envoyer(message: string): Observable<ReponseChatbot> {
    return this.http.post<ReponseChatbot>(`${environment.apiUrl}/chatbot`, { message });
  }
}
