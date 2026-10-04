export interface EntreeJournal {
  id: number;
  /** Note d'humeur de 1 à 10 */
  humeur: number;
  texte: string | null;
  tags: string[];
  /** Valence du texte calculée par l'IA, de 1 (négatif) à 5 (positif) */
  valence: number | null;
  /** Niveau de risque 0-3 (module 6) */
  niveauRisque: number | null;
  dateCreation: string;
}

export interface JournalRequest {
  humeur: number;
  texte?: string;
  tags?: string[];
}

export interface JournalCreation {
  entree: EntreeJournal;
  niveauRisque: number;
  /** Codes : urgences_3114, contact_therapeute_immediat, respiration_guidee, prise_rdv_prioritaire, meditation_courte, article_gestion_stress */
  recommandations: string[];
  baisseSignificative: boolean;
}

export interface PointHumeur {
  date: string;
  humeur: number;
  valence: number | null;
}

export type StatutTendance = 'amelioration' | 'stable' | 'degradation' | 'historique_insuffisant' | 'indisponible';

export interface Tendance {
  statut: StatutTendance;
  pente: number | null;
  moyenne7j: number | null;
  moyenne30j: number | null;
  baisseSignificative: boolean;
  points: PointHumeur[];
}

export const TAGS_EMOTION = ['calme', 'joie', 'gratitude', 'fatigue', 'stress', 'anxiété', 'tristesse', 'colère', 'solitude', 'motivation'];

export const STATUT_TENDANCE_LABELS: Record<StatutTendance, string> = {
  amelioration: 'En amélioration',
  stable: 'Stable',
  degradation: 'En baisse',
  historique_insuffisant: 'Pas encore assez de données (7 jours minimum)',
  indisponible: 'Analyse momentanément indisponible'
};

/** Libellé et lien des recommandations renvoyées par l'IA. */
export const RECOMMANDATIONS: Record<string, { label: string; icon: string; lien: string }> = {
  urgences_3114: { label: 'Appeler le 3114 (prévention du suicide, 24 h/24)', icon: 'icon-phone-call', lien: 'tel:3114' },
  contact_therapeute_immediat: { label: 'Écrire à votre thérapeute', icon: 'icon-message-circle', lien: '/messagerie' },
  respiration_guidee: { label: 'Exercice de respiration guidée', icon: 'icon-wind', lien: '/ressources' },
  prise_rdv_prioritaire: { label: 'Prendre rendez-vous avec un professionnel', icon: 'icon-calendar', lien: '/rendez-vous/prendre' },
  meditation_courte: { label: 'Méditation courte', icon: 'icon-sun', lien: '/ressources' },
  article_gestion_stress: { label: 'Article : gérer le stress', icon: 'icon-book-open', lien: '/ressources' }
};
