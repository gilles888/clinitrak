import { Routes } from '@angular/router';

/**
 * Routes du module Documents (GED MinIO).
 */
export const DOCUMENTS_ROUTES: Routes = [
  {
    path: '',
    loadComponent: () =>
      import('./document-list/document-list.component').then(m => m.DocumentListComponent),
    title: 'CliniTrak — Documents',
  },
];
