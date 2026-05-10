import { inject } from '@angular/core';
import { ActivatedRouteSnapshot, CanActivateFn, Router } from '@angular/router';
import { hasAnyRole, isAuthenticated } from '../store/auth.store';
import { SystemRole } from '../models/user.model';

/**
 * Guard fonctionnel Angular 20 qui vérifie que l'utilisateur possède les rôles requis.
 *
 * <p>Les rôles requis sont définis dans les données de la route via {@code data.roles}.
 *
 * Usage dans les routes :
 * ```ts
 * {
 *   path: 'admin',
 *   canActivate: [roleGuard],
 *   data: { roles: [SystemRole.ADMIN_TENANT, SystemRole.SUPER_ADMIN] },
 *   component: AdminComponent
 * }
 * ```
 */
export const roleGuard: CanActivateFn = (route: ActivatedRouteSnapshot) => {
  const router = inject(Router);

  if (!isAuthenticated()) {
    return router.createUrlTree(['/auth/login']);
  }

  const requiredRoles: SystemRole[] = route.data?.['roles'] ?? [];

  if (requiredRoles.length === 0) {
    // Aucun rôle requis : l'authentification suffit
    return true;
  }

  const hasAccess = hasAnyRole(...requiredRoles)();

  if (!hasAccess) {
    return router.createUrlTree(['/forbidden']);
  }

  return true;
};
