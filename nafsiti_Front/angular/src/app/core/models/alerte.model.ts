import { PersonneResume } from './rendez-vous.model';

export type SourceAlerte = 'JOURNAL' | 'MESSAGE';

export interface Alerte {
  id: number;
  patient: PersonneResume;
  source: SourceAlerte;
  /** 2 = élevé, 3 = critique */
  niveau: number;
  probabilite: number | null;
  /** « regle », « modele » ou « moderation » */
  origineDecision: string | null;
  dateCreation: string;
  traitee: boolean;
  traiteePar: PersonneResume | null;
  dateTraitement: string | null;
}

export const NIVEAU_RISQUE_LABELS: Record<number, string> = {
  0: 'Aucun signal',
  1: 'Modéré',
  2: 'Élevé',
  3: 'Critique'
};
