export type StatutRendezVous = 'EN_ATTENTE' | 'CONFIRME' | 'ANNULE';

export const STATUT_RDV_LABELS: Record<StatutRendezVous, string> = {
  EN_ATTENTE: 'En attente',
  CONFIRME: 'Confirmé',
  ANNULE: 'Annulé'
};

export interface PersonneResume {
  id: number;
  nom: string;
  prenom: string;
  email: string;
}

export interface Therapeute extends PersonneResume {
  creneauxLibres: number;
}

/** Créneau proposé par un thérapeute (dates ISO locales, ex. 2026-09-25T10:00:00). */
export interface Disponibilite {
  id: number;
  debut: string;
  fin: string;
  reserve: boolean;
}

export interface DisponibiliteRequest {
  debut: string;
  dureeMinutes: number;
}

export interface RendezVous {
  id: number;
  dateHeure: string;
  dateFin: string;
  statut: StatutRendezVous;
  motif: string | null;
  patient: PersonneResume;
  therapeute: PersonneResume;
  dateCreation: string;
}

export interface RendezVousRequest {
  disponibiliteId: number;
  motif?: string;
}

/** Regroupe des créneaux par jour (clé yyyy-MM-dd), dans l'ordre chronologique. */
export function grouperParJour<T extends { debut: string }>(creneaux: T[]): { jour: string; creneaux: T[] }[] {
  const groupes = new Map<string, T[]>();
  for (const c of creneaux) {
    const jour = c.debut.substring(0, 10);
    groupes.set(jour, [...(groupes.get(jour) ?? []), c]);
  }
  return [...groupes.entries()].map(([jour, liste]) => ({ jour, creneaux: liste }));
}

export function estPasse(dateIso: string): boolean {
  return new Date(dateIso).getTime() <= Date.now();
}
