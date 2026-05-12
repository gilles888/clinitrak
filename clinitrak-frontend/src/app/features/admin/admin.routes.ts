import { Routes } from '@angular/router';

/**
 * Routes du module Admin (administration multi-tenant).
 *
 * <p>Protégées par authGuard + roleGuard (ROLE_SUPER_ADMIN) dans app.routes.ts.
 * Chaque composant est chargé de manière lazy via loadComponent.
 */
export const ADMIN_ROUTES: Routes = [
  {
    path: '',
    loadComponent: () =>
      import('./system-dashboard/system-dashboard.component').then(m => m.SystemDashboardComponent),
    title: 'CliniTrak — Tableau de bord système',
  },
  {
    path: 'tenants',
    loadComponent: () =>
      import('./tenant-list/tenant-list.component').then(m => m.TenantListComponent),
    title: 'CliniTrak — Gestion des tenants',
  },
  {
    path: 'tenants/:id/config',
    loadComponent: () =>
      import('./tenant-config/tenant-config.component').then(m => m.TenantConfigComponent),
    title: 'CliniTrak — Configuration tenant',
  },
  {
    path: 'users',
    loadComponent: () =>
      import('./user-management/user-management.component').then(m => m.UserManagementComponent),
    title: 'CliniTrak — Gestion des utilisateurs',
  },
  {
    path: 'audit',
    loadComponent: () =>
      import('./audit-log-viewer/audit-log-viewer.component').then(m => m.AuditLogViewerComponent),
    title: 'CliniTrak — Journal d\'audit',
  },
];
