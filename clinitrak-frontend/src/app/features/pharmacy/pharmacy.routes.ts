import { Routes } from '@angular/router';

/**
 * Routes lazy-loaded du module Pharmacie de CliniTrak.
 *
 * <p>Chaque composant est chargé à la demande via {@code loadComponent}
 * pour optimiser le bundle initial.
 */
export const PHARMACY_ROUTES: Routes = [
  {
    path: '',
    loadComponent: () =>
      import('./pharmacy-dashboard/pharmacy-dashboard.component').then(
        m => m.PharmacyDashboardComponent,
      ),
    title: 'Pharmacie — Tableau de bord',
  },
  {
    path: 'stocks',
    loadComponent: () =>
      import('./drug-stock/drug-stock.component').then(
        m => m.DrugStockComponent,
      ),
    title: 'Pharmacie — Stocks',
  },
  {
    path: 'dispensations',
    loadComponent: () =>
      import('./dispensation-form/dispensation-form.component').then(
        m => m.DispensationFormComponent,
      ),
    title: 'Pharmacie — Dispensations',
  },
  {
    path: 'receive',
    loadComponent: () =>
      import('./stock-receipt/stock-receipt.component').then(
        m => m.StockReceiptComponent,
      ),
    title: 'Pharmacie — Réception de stock',
  },
  {
    path: 'alerts',
    loadComponent: () =>
      import('./expiry-alert/expiry-alert.component').then(
        m => m.ExpiryAlertComponent,
      ),
    title: 'Pharmacie — Alertes',
  },
  {
    path: 'emergency',
    loadComponent: () =>
      import('./emergency-unblinding/emergency-unblinding.component').then(
        m => m.EmergencyUnblindingComponent,
      ),
    title: 'Pharmacie — Levée d\'aveugle d\'urgence',
  },
  {
    path: 'reports',
    loadComponent: () =>
      import('./pharmacy-report/pharmacy-report.component').then(
        m => m.PharmacyReportComponent,
      ),
    title: 'Pharmacie — Rapports',
  },
];
