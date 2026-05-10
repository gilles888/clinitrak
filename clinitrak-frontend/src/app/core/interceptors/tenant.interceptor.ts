import { HttpHandlerFn, HttpInterceptorFn, HttpRequest } from '@angular/common/http';
import { environment } from '@env/environment';

/**
 * Intercepteur HTTP fonctionnel qui ajoute automatiquement le header {@code X-Tenant-ID}
 * à toutes les requêtes sortantes vers l'API.
 *
 * <p>Le tenant est résolu dans cet ordre de priorité :
 * <ol>
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
 * Résout le slug du tenant depuis le hostname ou l'environnement.
 *
 * @returns slug du tenant ou chaîne vide si non résolu
 */
function resolveTenantId(): string {
  // En dev, utiliser la valeur de l'environnement
  if (!environment.production) {
    return environment.tenantId;
  }

  // En prod : extraire le sous-domaine (ex: "saintluc" de "saintluc.clinitrak.be")
  const hostname = window.location.hostname;
  const parts = hostname.split('.');
  if (parts.length >= 3) {
    return parts[0]; // Premier segment = slug du tenant
  }

  return environment.tenantId;
}
