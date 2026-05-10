import { Routes } from '@angular/router';

/**
 * Routes lazy-loadées du module Études Cliniques.
 *
 * <p>Toutes les routes utilisent {@code loadComponent} pour un chargement à la demande.
 * Le paramètre {@code :id} est l'UUID de l'étude.
 *
 * <ul>
 *   <li>{@code /studies}          — liste des études avec filtres et pagination</li>
 *   <li>{@code /studies/new}      — formulaire de création d'une nouvelle étude</li>
 *   <li>{@code /studies/:id}      — détail d'une étude (6 onglets)</li>
 *   <li>{@code /studies/:id/edit} — formulaire de modification d'une étude</li>
 * </ul>
 */
export const STUDIES_ROUTES: Routes = [
  {
    path: '',
    loadComponent: () =>
      import('./study-list/study-list.component').then(m => m.StudyListComponent),
    title: 'CliniTrak — Études',
  },
  {
    path: 'new',
    loadComponent: () =>
      import('./study-form/study-form.component').then(m => m.StudyFormComponent),
    title: 'CliniTrak — Nouvelle étude',
  },
  {
    path: ':id',
    loadComponent: () =>
      import('./study-detail/study-detail.component').then(m => m.StudyDetailComponent),
    title: 'CliniTrak — Détail étude',
  },
  {
    path: ':id/edit',
    loadComponent: () =>
      import('./study-form/study-form.component').then(m => m.StudyFormComponent),
    title: 'CliniTrak — Modifier étude',
  },
];
