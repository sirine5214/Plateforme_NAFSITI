export type TypeRessource = 'MEDITATION' | 'RESPIRATION' | 'ARTICLE';

export const TYPES_RESSOURCE: TypeRessource[] = ['RESPIRATION', 'MEDITATION', 'ARTICLE'];

export const TYPE_RESSOURCE_LABELS: Record<TypeRessource, { label: string; icon: string }> = {
  RESPIRATION: { label: 'Respiration', icon: 'icon-wind' },
  MEDITATION: { label: 'Méditation', icon: 'icon-sun' },
  ARTICLE: { label: 'Article', icon: 'icon-book' }
};

export interface Ressource {
  id: number;
  titre: string;
  description: string;
  type: TypeRessource;
  contenu: string | null;
  dureeMinutes: number | null;
  url: string | null;
  valideParPro: boolean;
  popularite: number;
  aime: boolean;
  consultee: boolean;
}

export interface RessourceRequest {
  titre: string;
  description: string;
  type: TypeRessource;
  contenu?: string;
  dureeMinutes?: number | null;
  url?: string;
  valideParPro: boolean;
}

export interface Recommandations {
  ressources: Ressource[];
  /** Heure (0-23) conseillée pour le rappel quotidien */
  heureRappel: number;
  /** Faux : contenus populaires (nouvel utilisateur ou IA indisponible) */
  personnalise: boolean;
}
