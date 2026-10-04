import { PersonneResume } from './rendez-vous.model';
import { Role } from './utilisateur.model';

export type StatutMessage = 'PUBLIE' | 'MASQUE' | 'REJETE';

export interface Message {
  id: number;
  expediteurId: number;
  destinataireId: number;
  contenu: string;
  dateEnvoi: string;
  statut: StatutMessage;
  lu: boolean;
}

export interface Contact {
  personne: PersonneResume;
  role: Role;
  nonLus: number;
}

export interface EnvoiMessage {
  message: Message;
  decision: 'PUBLIER' | 'MASQUER_EN_ATTENTE_REVUE' | 'ESCALADE_HUMAINE' | 'NON_ANALYSE';
  /** Signal de crise détecté : afficher les numéros d'urgence */
  afficherUrgence: boolean;
}

export interface MessageModeration {
  id: number;
  expediteur: PersonneResume;
  destinataire: PersonneResume;
  contenu: string;
  dateEnvoi: string;
  toxicite: number | null;
}

/** Événement poussé par le serveur sur le WebSocket /ws. */
export interface EvenementTempsReel {
  type: 'PRET' | 'MESSAGE' | 'ALERTE' | 'MODERATION';
  donnees?: unknown;
}
