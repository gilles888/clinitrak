import { Routes } from '@angular/router';

/** Routes du module d'authentification (publiques). */
export const AUTH_ROUTES: Routes = [
  {
    path: 'login',
    loadComponent: () => import('./login/login.component').then(m => m.LoginComponent),
    title: 'CliniTrak — Connexion',
  },
  { path: '', redirectTo: 'login', pathMatch: 'full' },
];
