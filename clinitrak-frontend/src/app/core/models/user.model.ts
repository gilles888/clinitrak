/**
 * Modèles de données du domaine utilisateur (frontend).
 */

/** Représentation de l'utilisateur authentifié dans le store Angular. */
export interface CurrentUser {
  id: string;
  email: string;
  firstName: string;
  lastName: string;
  tenantId: string;
  roles: string[];
  /** Nom d'affichage complet. */
  readonly displayName: string;
}

/** Réponse du backend lors d'un login réussi. */
export interface AuthTokens {
  accessToken: string;
  refreshToken: string;
  tokenType: string;
  expiresIn: number;
  user: CurrentUser;
}

/** Requête de login. */
export interface LoginCredentials {
  email: string;
  password: string;
}

/** Rôles système disponibles dans CliniTrak. */
export enum SystemRole {
  SUPER_ADMIN    = 'ROLE_SUPER_ADMIN',
  ADMIN_TENANT   = 'ROLE_ADMIN_TENANT',
  CE_SECRETARY   = 'ROLE_CE_SECRETARY',
  CE_COORDINATOR = 'ROLE_CE_COORDINATOR',
  CTC_DESK       = 'ROLE_CTC_DESK',
  CTC_CRA        = 'ROLE_CTC_CRA',
  CTC_PM         = 'ROLE_CTC_PM',
  CTC_COFI       = 'ROLE_CTC_COFI',
  PHARMACIST     = 'ROLE_PHARMACIST',
  INVESTIGATOR   = 'ROLE_INVESTIGATOR',
  EXTERNAL       = 'ROLE_EXTERNAL',
}

/** État d'authentification géré par le store de signals. */
export interface AuthState {
  user: CurrentUser | null;
  accessToken: string | null;
  refreshToken: string | null;
  isAuthenticated: boolean;
  isLoading: boolean;
  error: string | null;
}
