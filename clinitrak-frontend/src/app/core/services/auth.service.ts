import { inject, Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Router } from '@angular/router';
import { Observable, tap, catchError, throwError } from 'rxjs';
import { environment } from '@env/environment';
import { AuthTokens, LoginCredentials } from '../models/user.model';
import {
  clearAuthState,
  setAuthenticated,
  setAuthError,
  setLoading,
  updateAccessToken,
} from '../store/auth.store';

/**
 * Service d'authentification Angular.
 *
 * <p>Gère le cycle de vie complet côté frontend :
 * login → stockage tokens → refresh → logout.
 *
 * <p>Les tokens sont stockés dans localStorage (persistance entre onglets).
 * Le refresh token ne transite jamais dans les headers — il est envoyé
 * uniquement via l'endpoint /auth/refresh en body JSON.
 */
@Injectable({ providedIn: 'root' })
export class AuthService {

  private readonly http = inject(HttpClient);
  private readonly router = inject(Router);

  private readonly AUTH_URL = `${environment.apiBaseUrl}/v1/auth`;
  private readonly ACCESS_TOKEN_KEY  = 'ct_access_token';
  private readonly REFRESH_TOKEN_KEY = 'ct_refresh_token';

  /**
   * Authentifie l'utilisateur et stocke les tokens.
   *
   * @param credentials email + password
   * @returns Observable de la réponse d'authentification
   */
  login(credentials: LoginCredentials): Observable<AuthTokens> {
    setLoading(true);
    return this.http.post<AuthTokens>(`${this.AUTH_URL}/login`, credentials).pipe(
      tap(response => {
        this.storeTokens(response.accessToken, response.refreshToken);
        setAuthenticated(response.user, response.accessToken, response.refreshToken);
      }),
      catchError(err => {
        setAuthError(err.error?.detail ?? 'Identifiants invalides');
        return throwError(() => err);
      })
    );
  }

  /**
   * Rafraîchit l'access token via le refresh token stocké.
   *
   * @returns Observable du nouveau couple de tokens
   */
  refreshToken(): Observable<AuthTokens> {
    const refreshToken = this.getStoredRefreshToken();
    if (!refreshToken) {
      this.logout();
      return throwError(() => new Error('Aucun refresh token disponible'));
    }

    return this.http.post<AuthTokens>(`${this.AUTH_URL}/refresh`, { refreshToken }).pipe(
      tap(response => {
        this.storeTokens(response.accessToken, response.refreshToken);
        updateAccessToken(response.accessToken);
      }),
      catchError(err => {
        this.logout();
        return throwError(() => err);
      })
    );
  }

  /**
   * Déconnecte l'utilisateur : révoque les tokens côté serveur et nettoie le state.
   */
  logout(): void {
    const refreshToken = this.getStoredRefreshToken();
    if (refreshToken) {
      // Appel best-effort (ne bloque pas si le serveur est indisponible)
      this.http.post(`${this.AUTH_URL}/logout`, {}).subscribe({ error: () => {} });
    }
    this.clearStoredTokens();
    clearAuthState();
    this.router.navigate(['/auth/login']);
  }

  /**
   * Initialise l'état depuis le localStorage (restauration après rechargement de page).
   *
   * Appelé dans APP_INITIALIZER.
   */
  initFromStorage(): void {
    const token = this.getStoredAccessToken();
    if (!token) return;

    // Décode le payload JWT sans librairie externe (simple base64)
    try {
      const payload = JSON.parse(atob(token.split('.')[1]));
      if (payload.exp * 1000 > Date.now()) {
        // Token encore valide — on pourrait appeler /me pour récupérer le profil
        // Pour l'instant on reconstruit l'état minimal depuis le payload
        setAuthenticated(
          {
            id:          payload.userId,
            email:       payload.sub,
            firstName:   payload.firstName ?? '',
            lastName:    payload.lastName ?? '',
            tenantId:    payload.tenantId,
            roles:       payload.roles ?? [],
            displayName: `${payload.firstName ?? ''} ${payload.lastName ?? ''}`.trim(),
          },
          token,
          this.getStoredRefreshToken() ?? ''
        );
      } else {
        // Token expiré — tenter un refresh
        this.refreshToken().subscribe({ error: () => {} });
      }
    } catch {
      this.clearStoredTokens();
    }
  }

  /** Retourne l'access token stocké. */
  getStoredAccessToken(): string | null {
    return localStorage.getItem(this.ACCESS_TOKEN_KEY);
  }

  /** Retourne le refresh token stocké. */
  getStoredRefreshToken(): string | null {
    return localStorage.getItem(this.REFRESH_TOKEN_KEY);
  }

  private storeTokens(accessToken: string, refreshToken: string): void {
    localStorage.setItem(this.ACCESS_TOKEN_KEY, accessToken);
    localStorage.setItem(this.REFRESH_TOKEN_KEY, refreshToken);
  }

  private clearStoredTokens(): void {
    localStorage.removeItem(this.ACCESS_TOKEN_KEY);
    localStorage.removeItem(this.REFRESH_TOKEN_KEY);
  }
}
