import { Routes } from '@angular/router';

/**
 * Routes du module Exchange (portail externe public).
 *
 * <p>Toutes les routes sont publiques — pas d'authGuard interne.
 * Les composants sont chargés de manière lazy via loadComponent.
 */
export const EXCHANGE_ROUTES: Routes = [
  {
    path: '',
    loadComponent: () =>
      import('./landing-page/landing-page.component').then(m => m.LandingPageComponent),
    title: 'CliniTrak Exchange',
  },
  {
    path: 'register',
    loadComponent: () =>
      import('./external-registration/external-registration.component').then(m => m.ExternalRegistrationComponent),
    title: 'CliniTrak Exchange — Inscription',
  },
  {
    path: 'login',
    loadComponent: () =>
      import('./exchange-login/exchange-login.component').then(m => m.ExchangeLoginComponent),
    title: 'CliniTrak Exchange — Connexion',
  },
  {
    path: 'submit',
    loadComponent: () =>
      import('./request-wizard/request-wizard.component').then(m => m.RequestWizardComponent),
    title: 'CliniTrak Exchange — Nouvelle demande',
  },
  {
    path: 'tracking',
    loadComponent: () =>
      import('./request-tracking/request-tracking.component').then(m => m.RequestTrackingComponent),
    title: 'CliniTrak Exchange — Suivi',
  },
  {
    path: 'tracking/:id',
    loadComponent: () =>
      import('./request-tracking/request-tracking.component').then(m => m.RequestTrackingComponent),
    title: 'CliniTrak Exchange — Détail demande',
  },
  {
    path: 'messages/:requestId',
    loadComponent: () =>
      import('./messaging/messaging.component').then(m => m.MessagingComponent),
    title: 'CliniTrak Exchange — Messagerie',
  },
];
