import { computed, signal } from '@angular/core';
import { AuthState, CurrentUser, SystemRole } from '../models/user.model';

/**
 * Store réactif pour l'état d'authentification, basé sur les Signals Angular.
 *
 * Usage :
 * ```ts
 * import { authState, currentUser, isAuthenticated, hasRole } from '@core/store/auth.store';
 *
 * // Dans un composant :
 * const user = currentUser();
 * const canAdmin = hasRole(SystemRole.ADMIN_TENANT)();
 * ```
 */

const INITIAL_STATE: AuthState = {
  user:            null,
  accessToken:     null,
  refreshToken:    null,
  isAuthenticated: false,
  isLoading:       false,
  error:           null,
};

/** Signal racine de l'état d'authentification. */
export const authState = signal<AuthState>(INITIAL_STATE);

// -------------------------
// Computed signals (dérivés)
// -------------------------

/** Utilisateur courant ou null. */
export const currentUser = computed<CurrentUser | null>(() => authState().user);

/** true si l'utilisateur est connecté. */
export const isAuthenticated = computed(() => authState().isAuthenticated);

/** Access token JWT courant. */
export const accessToken = computed(() => authState().accessToken);

/** true pendant un chargement (login, refresh). */
export const isLoading = computed(() => authState().isLoading);

/** Message d'erreur courant. */
export const authError = computed(() => authState().error);

/**
 * Retourne un signal computed indiquant si l'utilisateur possède le rôle demandé.
 *
 * @param role rôle à vérifier
 * @returns signal boolean
 */
export const hasRole = (role: SystemRole) =>
  computed(() => authState().user?.roles.includes(role) ?? false);

/**
 * Retourne un signal computed indiquant si l'utilisateur possède au moins un des rôles.
 *
 * @param roles liste de rôles (OR logique)
 * @returns signal boolean
 */
export const hasAnyRole = (...roles: SystemRole[]) =>
  computed(() => {
    const userRoles = authState().user?.roles ?? [];
    return roles.some(r => userRoles.includes(r));
  });

// -------------------------
// Mutateurs d'état
// -------------------------

/** Positionne l'état après un login réussi. */
export function setAuthenticated(
  user: CurrentUser,
  accessToken: string,
  refreshToken: string
): void {
  authState.set({
    user,
    accessToken,
    refreshToken,
    isAuthenticated: true,
    isLoading: false,
    error: null,
  });
}

/** Met à jour uniquement l'access token (après un refresh). */
export function updateAccessToken(newToken: string): void {
  authState.update(state => ({ ...state, accessToken: newToken }));
}

/** Réinitialise l'état après un logout. */
export function clearAuthState(): void {
  authState.set(INITIAL_STATE);
}

/** Positionne le flag de chargement. */
export function setLoading(loading: boolean): void {
  authState.update(state => ({ ...state, isLoading: loading, error: null }));
}

/** Positionne un message d'erreur. */
export function setAuthError(error: string): void {
  authState.update(state => ({ ...state, isLoading: false, error }));
}
