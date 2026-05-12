import { HttpHandlerFn, HttpInterceptorFn, HttpRequest } from '@angular/common/http';
import { environment } from '@env/environment';

const TENANT_OVERRIDE_KEY = 'ct_tenant_override';

/**
 * Intercepteur HTTP fonctionnel qui ajoute automatiquement le header {@code X-Tenant-ID}
 * à toutes les requêtes sortantes vers l'API.
 *
 * <p>Le tenant est résolu dans cet ordre de priorité :
 * <ol>
 *   <li>{@code localStorage[ct_tenant_override]} — sélection manuelle SUPER_ADMIN</li>
 *   <li>Sous-domaine de l'URL courante (ex: {@code saintluc.clinitrak.be} → {@code saintluc})</li>
 *   <li>Variable d'environnement {@code environment.tenantId}</li>
 * </ol>
 *
 * @param req requête HTTP entrante
 * @param next handler suivant dans la chaîne
 * @returns Observable de la réponse HTTP enrichie du header tenant
 */
export const tenantInterceptor: HttpInterceptorFn = (
  req: HttpRequest<unknown>,
  next: HttpHandlerFn
) => {
  const tenantId = resolveTenantId();

  if (!tenantId) {
    return next(req);
  }

  const tenantReq = req.clone({
    setHeaders: { 'X-Tenant-ID': tenantId },
  });

  return next(tenantReq);
};

/**
 * Résout le slug du tenant depuis localStorage, le hostname ou l'environnement.
 *
 * @returns slug du tenant ou chaîne vide si non résolu
 */
function resolveTenantId(): string {
  // Priorité 1 : override SUPER_ADMIN stocké en localStorage
  const override = localStorage.getItem(TENANT_OVERRIDE_KEY);
  if (override) {
    return override;
  }

  // Priorité 2 (prod) : sous-domaine (ex: "saintluc" de "saintluc.clinitrak.be")
  if (environment.production) {
    const parts = window.location.hostname.split('.');
    if (parts.length >= 3) {
      return parts[0];
    }
  }

  // Priorité 3 : valeur statique de l'environnement (dev)
  return environment.tenantId;
}
