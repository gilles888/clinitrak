import { Routes } from '@angular/router';

/**
 * Routes du module Comité d'Éthique, chargées en lazy loading.
 *
 * <p>Toutes les routes utilisent {@code loadComponent} pour garantir le lazy loading
 * et réduire la taille du bundle initial.
 */
export const ETHICS_ROUTES: Routes = [
  {
    path: '',
    loadComponent: () => import('./ethics-dashboard/ethics-dashboard.component').then(m => m.EthicsDashboardComponent),
    title: 'CliniTrak — Comité d\'Éthique',
  },
  {
    path: 'reviews',
    loadComponent: () => import('./ethics-study-list/ethics-study-list.component').then(m => m.EthicsStudyListComponent),
    title: 'CliniTrak — Avis CE',
  },
  {
    path: 'reviews/new',
    loadComponent: () => import('./ethics-review-form/ethics-review-form.component').then(m => m.EthicsReviewFormComponent),
    title: 'CliniTrak — Nouvel avis CE',
  },
  {
    path: 'reviews/:id/decision',
    loadComponent: () => import('./ethics-review-form/ethics-review-form.component').then(m => m.EthicsReviewFormComponent),
    title: 'CliniTrak — Décision CE',
  },
  {
    path: 'meetings',
    loadComponent: () => import('./meeting-calendar/meeting-calendar.component').then(m => m.MeetingCalendarComponent),
    title: 'CliniTrak — Réunions CE',
  },
  {
    path: 'meetings/:id',
    loadComponent: () => import('./meeting-detail/meeting-detail.component').then(m => m.MeetingDetailComponent),
    title: 'CliniTrak — Détail réunion',
  },
  {
    path: 'correspondence',
    loadComponent: () => import('./correspondence-generator/correspondence-generator.component').then(m => m.CorrespondenceGeneratorComponent),
    title: 'CliniTrak — Correspondances',
  },
  {
    path: 'annual-reports',
    loadComponent: () => import('./annual-report-tracker/annual-report-tracker.component').then(m => m.AnnualReportTrackerComponent),
    title: 'CliniTrak — Rapports annuels',
  },
];
