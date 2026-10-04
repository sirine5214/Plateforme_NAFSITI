export type Role = 'PATIENT' | 'THERAPEUTE' | 'ADMINISTRATEUR';

export const ROLES: Role[] = ['PATIENT', 'THERAPEUTE', 'ADMINISTRATEUR'];

export const ROLE_LABELS: Record<Role, string> = {
  PATIENT: 'Patient',
  THERAPEUTE: 'Thérapeute',
  ADMINISTRATEUR: 'Administrateur'
};

export interface Utilisateur {
  id: number;
  nom: string;
  prenom: string;
  email: string;
  role: Role;
  actif: boolean;
  dateCreation: string;
  dateConsentement: string | null;
  /** Le patient accepte que ses alertes de détresse soient transmises à ses thérapeutes. */
  partageAlertes: boolean;
  // Profil thérapeute (matching IA)
  specialites: string | null;
  approche: string | null;
  langues: string | null;
}

export interface LoginRequest {
  email: string;
  motDePasse: string;
}

export interface RegisterRequest {
  nom: string;
  prenom: string;
  email: string;
  motDePasse: string;
  role: Exclude<Role, 'ADMINISTRATEUR'>;
  /** Consentement RGPD explicite */
  consentement: boolean;
}

/** Connexion par reconnaissance faciale : email + capture webcam (data URL JPEG). */
export interface LoginVisageRequest {
  email: string;
  image: string;
}

/**
 * Si mfaRequis est vrai (connexion jugée inhabituelle par l'IA), aucun token n'est délivré :
 * il faut confirmer avec son visage (mfaToken valable 5 minutes).
 */
export interface AuthResponse {
  token: string | null;
  type: string | null;
  expiresIn: number;
  utilisateur: Utilisateur | null;
  mfaRequis: boolean;
  mfaToken: string | null;
}

export interface VisageStatut {
  enregistre: boolean;
  dateEnregistrement: string | null;
}

export interface ProfilTherapeuteRequest {
  specialites: string;
  approche: string;
  langues: string;
}

/** Création / modification par l'administrateur (mot de passe optionnel en modification). */
export interface UtilisateurRequest {
  nom: string;
  prenom: string;
  email: string;
  motDePasse?: string;
  role: Role;
  actif: boolean;
}

export interface ProfilRequest {
  nom: string;
  prenom: string;
  email: string;
}

export interface ChangementMotDePasseRequest {
  ancienMotDePasse: string;
  nouveauMotDePasse: string;
}

export interface UtilisateurStats {
  total: number;
  patients: number;
  therapeutes: number;
  administrateurs: number;
  actifs: number;
  inactifs: number;
}

export interface UtilisateurFiltres {
  recherche?: string;
  role?: Role | '';
  actif?: boolean | '';
}

/** Même règle que le backend (PasswordRules) : 8 caractères min., au moins une lettre et un chiffre. */
export const PASSWORD_PATTERN = /^(?=.*[A-Za-z])(?=.*\d).{8,}$/;

export function initiales(u: Pick<Utilisateur, 'nom' | 'prenom'> | null | undefined): string {
  if (!u) return '';
  return `${u.prenom?.charAt(0) ?? ''}${u.nom?.charAt(0) ?? ''}`.toUpperCase();
}
