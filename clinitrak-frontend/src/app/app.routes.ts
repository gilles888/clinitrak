import { Routes } from '@angular/router';
import { authGuard } from './core/guards/auth.guard';
import { roleGuard } from './core/guards/role.guard';
import { SystemRole } from './core/models/user.model';

/**
 * Configuration des routes principales de CliniTrak.
 *
 * <p>Architecture :
 * <ul>
 *   <li>/auth/**         — routes publiques (login, MFA, reset)</li>
 *   <li>/                 — layout principal protégé par authGuard</li>
 *   <li>/dashboard        — tableau de bord personnalisé par rôle</li>
 *   <li>/studies          — module Études cliniques</li>
 *   <li>/ethics           — module Comité d'Éthique</li>
 *   <li>/ctc              — module CTC</li>
 *   <li>/pharmacy         — module Pharmacie</li>
 *   <li>/billing          — module Facturation</li>
 *   <li>/documents        — module Documents</li>
 *   <li>/admin            — administration tenant (rôle ADMIN requis)</li>
 * </ul>
 */
export const routes: Routes = [
  // Routes publiques (pas de layout principal)
  {
    path: 'auth',
    loadChildren: () => import('./features/auth/auth.routes').then(m => m.AUTH_ROUTES),
  },

  // Portail Exchange externe (public — pas d'authGuard interne)
  {
    path: 'exchange',
    loadChildren: () => import('./features/exchange/exchange.routes').then(m => m.EXCHANGE_ROUTES),
    title: 'CliniTrak Exchange',
  },

  // Layout principal (protégé)
  {
    path: '',
    loadComponent: () =>
      import('./layout/main-layout/main-layout.component').then(m => m.MainLayoutComponent),
    canActivate: [authGuard],
    children: [
      { path: '', redirectTo: 'dashboard', pathMatch: 'full' },

      {
        path: 'dashboard',
        loadComponent: () =>
          import('./features/dashboard/dashboard.component').then(m => m.DashboardComponent),
        title: 'CliniTrak — Tableau de bord',
      },

      {
        path: 'studies',
        loadChildren: () =>
          import('./features/studies/studies.routes').then(m => m.STUDIES_ROUTES),
        title: 'CliniTrak — Études',
      },

      {
        path: 'ethics',
        loadChildren: () =>
          import('./features/ethics/ethics.routes').then(m => m.ETHICS_ROUTES),
        canActivate: [roleGuard],
        data: { roles: [SystemRole.CE_SECRETARY, SystemRole.CE_COORDINATOR, SystemRole.ADMIN_TENANT, SystemRole.SUPER_ADMIN] },
        title: 'CliniTrak — Comité d\'Éthique',
      },

      {
        path: 'ctc',
        loadChildren: () =>
          import('./features/ctc/ctc.routes').then(m => m.CTC_ROUTES),
        canActivate: [roleGuard],
        data: { roles: [SystemRole.CTC_DESK, SystemRole.CTC_CRA, SystemRole.CTC_PM, SystemRole.CTC_COFI, SystemRole.ADMIN_TENANT, SystemRole.SUPER_ADMIN] },
        title: 'CliniTrak — CTC',
      },

      {
        path: 'pharmacy',
        loadChildren: () =>
          import('./features/pharmacy/pharmacy.routes').then(m => m.PHARMACY_ROUTES),
        canActivate: [roleGuard],
        data: { roles: [SystemRole.PHARMACIST, SystemRole.ADMIN_TENANT, SystemRole.SUPER_ADMIN] },
        title: 'CliniTrak — Pharmacie',
      },

      {
        path: 'billing',
        loadChildren: () =>
          import('./features/billing/billing.routes').then(m => m.BILLING_ROUTES),
        canActivate: [roleGuard],
        data: { roles: [SystemRole.CTC_COFI, SystemRole.ADMIN_TENANT, SystemRole.SUPER_ADMIN] },
        title: 'CliniTrak — Facturation',
      },

      {
        path: 'documents',
        loadChildren: () =>
          import('./features/documents/documents.routes').then(m => m.DOCUMENTS_ROUTES),
        title: 'CliniTrak — Documents',
      },

      {
        path: 'notifications',
        loadComponent: () =>
          import('./features/notifications/notifications.component').then(m => m.NotificationsComponent),
        title: 'CliniTrak — Notifications',
      },

      {
        path: 'admin',
        loadChildren: () =>
          import('./features/admin/admin.routes').then(m => m.ADMIN_ROUTES),
        canActivate: [roleGuard],
        data: { roles: [SystemRole.ADMIN_TENANT, SystemRole.SUPER_ADMIN] },
        title: 'CliniTrak — Administration',
      },
    ],
  },

  { path: 'forbidden', loadComponent: () => import('./shared/components/forbidden/forbidden.component').then(m => m.ForbiddenComponent) },
  { path: '**', redirectTo: 'dashboard' },
];
