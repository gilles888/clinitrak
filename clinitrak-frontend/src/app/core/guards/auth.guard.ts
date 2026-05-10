import { inject } from '@angular/core';
import { CanActivateFn, Router } from '@angular/router';
import { isAuthenticated } from '../store/auth.store';

/**
 * Guard fonctionnel Angular 20 qui protège les routes nécessitant une authentification.
 *
 * <p>Redirige vers {@code /auth/login} si l'utilisateur n'est pas connecté,
 * en conservant l'URL demandée dans le paramètre {@code returnUrl}.
 *
 * Usage dans les routes :
 * ```ts
 * { path: 'dashboard', canActivate: [authGuard], component: DashboardComponent }
 * ```
 */
export const authGuard: CanActivateFn = (route, state) => {
  const router = inject(Router);

  if (isAuthenticated()) {
    return true;
  }

  return router.createUrlTree(['/auth/login'], {
    queryParams: { returnUrl: state.url },
  });
};
