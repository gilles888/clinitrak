import { Routes } from '@angular/router';

/**
 * Routes du module CTC (Clinical Trial Center), chargées en lazy loading.
 *
 * <p>Toutes les routes utilisent {@code loadComponent} pour garantir le lazy loading
 * et réduire la taille du bundle initial.
 */
export const CTC_ROUTES: Routes = [
  {
    path: '',
    loadComponent: () =>
      import('./ctc-dashboard/ctc-dashboard.component').then(m => m.CTCDashboardComponent),
    title: 'CliniTrak — CTC',
  },
  {
    path: 'desk-requests',
    loadComponent: () =>
      import('./desk-request-list/desk-request-list.component').then(m => m.DeskRequestListComponent),
    title: 'CliniTrak — Demandes guichet',
  },
  {
    path: 'monitoring',
    loadComponent: () =>
      import('./monitoring-plan/monitoring-plan.component').then(m => m.MonitoringPlanComponent),
    title: 'CliniTrak — Plan de monitoring',
  },
  {
    path: 'financials',
    loadComponent: () =>
      import('./financial-dashboard/financial-dashboard.component').then(m => m.FinancialDashboardComponent),
    title: 'CliniTrak — Contrats financiers',
  },
  {
    path: 'quality',
    loadComponent: () =>
      import('./quality-tracker/quality-tracker.component').then(m => m.QualityTrackerComponent),
    title: 'CliniTrak — Qualité',
  },
  {
    path: 'sponsor-studies',
    loadComponent: () =>
      import('./sponsor-study-dashboard/sponsor-study-dashboard.component').then(m => m.SponsorStudyDashboardComponent),
    title: 'CliniTrak — Études promoteur',
  },
  {
    path: 'statistics',
    loadComponent: () =>
      import('./statistics-request-form/statistics-request-form.component').then(m => m.StatisticsRequestFormComponent),
    title: 'CliniTrak — Statistiques',
  },
  {
    path: 'study/:studyId',
    loadComponent: () =>
      import('./ctc-study-detail/ctc-study-detail.component').then(m => m.CTCStudyDetailComponent),
    title: 'CliniTrak — Détail étude CTC',
  },
  {
    path: 'study/:studyId/timeline',
    loadComponent: () =>
      import('./study-timeline/study-timeline.component').then(m => m.StudyTimelineComponent),
    title: 'CliniTrak — Timeline étude',
  },
];
