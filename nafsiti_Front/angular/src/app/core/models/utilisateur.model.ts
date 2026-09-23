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
}

export interface AuthResponse {
  token: string;
  type: string;
  expiresIn: number;
  utilisateur: Utilisateur;
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
