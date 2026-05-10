import { HttpHandlerFn, HttpInterceptorFn, HttpRequest } from '@angular/common/http';
import { inject } from '@angular/core';
import { catchError, switchMap, throwError } from 'rxjs';
import { AuthService } from '../services/auth.service';
import { isAuthenticated } from '../store/auth.store';

/**
 * Intercepteur HTTP fonctionnel qui ajoute automatiquement le JWT Bearer token
 * à chaque requête sortante vers l'API.
 *
 * <p>En cas de réponse 401, tente un refresh automatique du token (une seule fois)
 * avant de répercuter l'erreur.
 *
 * @param req requête HTTP entrante
 * @param next handler suivant dans la chaîne
 * @returns Observable de la réponse HTTP
 */
export const authInterceptor: HttpInterceptorFn = (
  req: HttpRequest<unknown>,
  next: HttpHandlerFn
) => {
  const authService = inject(AuthService);

  const token = authService.getStoredAccessToken();

  if (!token || !isAuthenticated()) {
    return next(req);
  }

  const authReq = addBearerToken(req, token);

  return next(authReq).pipe(
    catchError(error => {
      if (error.status === 401) {
        // Tente un refresh silencieux puis rejoue la requête originale
        return authService.refreshToken().pipe(
          switchMap(tokens => {
            const retryReq = addBearerToken(req, tokens.accessToken);
            return next(retryReq);
          }),
          catchError(refreshError => {
            authService.logout();
            return throwError(() => refreshError);
          })
        );
      }
      return throwError(() => error);
    })
  );
};

/** Ajoute le header Authorization: Bearer <token> à la requête. */
function addBearerToken(req: HttpRequest<unknown>, token: string): HttpRequest<unknown> {
  return req.clone({
    setHeaders: { Authorization: `Bearer ${token}` },
  });
}
