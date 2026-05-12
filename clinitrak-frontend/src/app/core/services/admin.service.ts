import { inject, Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { AdminTenant, AuditLog, SystemHealth, TenantStatistics } from '../models/admin.model';

/**
 * Service Angular pour l'administration multi-tenant.
 *
 * <p>Expose les opérations de gestion des tenants, des logs d'audit
 * et de la santé du système. Requiert le rôle ROLE_SUPER_ADMIN.
 */
@Injectable({ providedIn: 'root' })
export class AdminService {

  private readonly http = inject(HttpClient);
  private readonly base = '/api/v1/admin';

  // ─────────────────────── Tenants ───────────────────────

  /**
   * Récupère la liste de tous les tenants.
   *
   * @returns Liste des tenants
   */
  getTenants(): Observable<AdminTenant[]> {
    return this.http.get<AdminTenant[]>(`${this.base}/tenants`);
  }

  /**
   * Crée un nouveau tenant.
   *
   * @param req Données du tenant à créer
   * @returns Le tenant créé
   */
  createTenant(req: Partial<AdminTenant>): Observable<AdminTenant> {
    return this.http.post<AdminTenant>(`${this.base}/tenants`, req);
  }

  /**
   * Met à jour la configuration d'un tenant existant.
   *
   * @param id Identifiant du tenant
   * @param config Map de clés/valeurs de configuration
   * @returns Le tenant mis à jour
   */
  updateTenantConfig(id: string, config: Record<string, unknown>): Observable<AdminTenant> {
    return this.http.patch<AdminTenant>(`${this.base}/tenants/${id}/config`, config);
  }

  /**
   * Récupère les statistiques d'utilisation d'un tenant.
   *
   * @param id Identifiant du tenant
   * @returns Les statistiques du tenant
   */
  getTenantStatistics(id: string): Observable<TenantStatistics> {
    return this.http.get<TenantStatistics>(`${this.base}/tenants/${id}/statistics`);
  }

  /**
   * Invite un utilisateur dans un tenant.
   *
   * @param tenantId Identifiant du tenant
   * @param req Données de l'invitation (email, firstName, lastName, role)
   * @returns Statut de l'invitation
   */
  inviteUser(tenantId: string, req: Record<string, unknown>): Observable<{ email: string; status: string }> {
    return this.http.post<{ email: string; status: string }>(
      `${this.base}/tenants/${tenantId}/invite`,
      req
    );
  }

  // ─────────────────────── Audit ───────────────────────

  /**
   * Récupère les entrées du journal d'audit.
   *
   * @param params Filtres optionnels (tenantId, action, from, to, page, size)
   * @returns Liste des entrées d'audit
   */
  getAuditLogs(params?: Record<string, string | number>): Observable<AuditLog[]> {
    return this.http.get<AuditLog[]>(`${this.base}/audit`, { params });
  }

  /**
   * Exporte le journal d'audit au format Excel (blob binaire).
   *
   * @returns Blob du fichier Excel
   */
  exportAuditLogs(): Observable<Blob> {
    return this.http.get(`${this.base}/audit/export`, { responseType: 'blob' });
  }

  // ─────────────────────── Système ───────────────────────

  /**
   * Récupère l'état de santé de tous les services du système.
   *
   * @returns L'état de santé global
   */
  getSystemHealth(): Observable<SystemHealth> {
    return this.http.get<SystemHealth>(`${this.base}/health`);
  }
}
