import { Routes } from '@angular/router';

/**
 * Routes du module Facturation (a implementer).
 */
export const BILLING_ROUTES: Routes = [
  {
    path: '',
    loadComponent: () =>
      import('./billing.component').then(m => m.BillingComponent),
    title: 'CliniTrak — Facturation',
  },
];
