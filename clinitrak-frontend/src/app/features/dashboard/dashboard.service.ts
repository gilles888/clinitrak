import { inject, Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { forkJoin, Observable, of } from 'rxjs';
import { catchError, map } from 'rxjs/operators';
import { environment } from '../../../environments/environment';
import { StudyService } from '../../core/services/study.service';
import {
  CtcStatsSummary,
  DashboardData,
  DashboardStats,
  EthicsStatsSummary,
  PharmacyStatsSummary,
  StudyStatsSummary,
  TimelineItem,
  UrgentTask,
} from './dashboard.models';

/**
 * Service de données du tableau de bord CliniTrak.
 *
 * <p>Agrège les statistiques de tous les modules via {@link forkJoin}.
 * Tous les modules disposant d'un endpoint dédié utilisent les vraies données :
 * study-service, ethics-service, ctc-service et pharmacy-service.
 * En cas d'indisponibilité d'un service, des valeurs de repli (fallback) sont retournées.
 *
 * @example
 * ```ts
 * private readonly dashboardService = inject(DashboardService);
 *
 * this.dashboardService.loadAll().subscribe(data => {
 *   this.stats.set(data.stats);
 *   this.urgentTasks.set(data.urgentTasks);
 * });
 * ```
 */
@Injectable({ providedIn: 'root' })
export class DashboardService {

  private readonly http         = inject(HttpClient);
  private readonly studyService = inject(StudyService);

  /** URL de base de l'API gateway (préfixe commun à tous les services). */
  private readonly apiBaseUrl = `${environment.apiBaseUrl}/v1`;

  // ─────────────────────────────────────────────────────────────
  // Entrée principale — charge toutes les données en parallèle
  // ─────────────────────────────────────────────────────────────

  /**
   * Charge l'ensemble des données du tableau de bord en parallèle.
   *
   * @returns Observable<DashboardData> combinant stats, tâches urgentes et timeline
   */
  loadAll(): Observable<DashboardData> {
    return forkJoin({
      studyStats:    this.getStudyStats(),
      ethicsStats:   this.getEthicsStats(),
      ctcStats:      this.getCtcStats(),
      pharmacyStats: this.getPharmacyStats(),
      urgentTasks:   of(this.getUrgentTasks()),
      timeline:      of(this.getTimeline()),
    }).pipe(
      map(result => ({
        stats: {
          studyStats:    result.studyStats,
          ethicsStats:   result.ethicsStats,
          ctcStats:      result.ctcStats,
          pharmacyStats: result.pharmacyStats,
        } as DashboardStats,
        urgentTasks: result.urgentTasks,
        timeline:    result.timeline,
      }))
    );
  }

  // ─────────────────────────────────────────────────────────────
  // Statistiques études — données réelles via StudyService
  // ─────────────────────────────────────────────────────────────

  /**
   * Récupère les statistiques des études cliniques depuis le study-service.
   * En cas d'erreur réseau, retourne des valeurs par défaut.
   *
   * @returns Observable<StudyStatsSummary>
   */
  getStudyStats(): Observable<StudyStatsSummary> {
    return this.studyService.getStatistics().pipe(
      map(stats => ({
        total:   stats.totalStudies,
        active:  stats.ongoingStudies + stats.approvedStudies,
        pending: stats.draftStudies + (stats.totalStudies - stats.ongoingStudies - stats.approvedStudies - stats.draftStudies - stats.closedStudies),
        trend:   12,
      })),
      catchError(() => of<StudyStatsSummary>({
        total:   0,
        active:  0,
        pending: 0,
        trend:   0,
      }))
    );
  }

  // ─────────────────────────────────────────────────────────────
  // Statistiques CE — données réelles via ethics-service
  // ─────────────────────────────────────────────────────────────

  /**
   * Récupère les statistiques du Comité d'Éthique depuis le ethics-service.
   * Endpoint : GET /api/v1/ethics/dashboard
   * En cas d'erreur réseau, retourne des valeurs de repli à zéro.
   *
   * @returns Observable<EthicsStatsSummary>
   */
  getEthicsStats(): Observable<EthicsStatsSummary> {
    return this.http.get<EthicsStatsSummary>(`${this.apiBaseUrl}/ethics/dashboard`).pipe(
      catchError(() => of<EthicsStatsSummary>({
        pendingSubmissions:    0,
        urgentDeadlines:       0,
        averageProcessingDays: 0,
      }))
    );
  }

  // ─────────────────────────────────────────────────────────────
  // Statistiques CTC — données réelles via ctc-service
  // ─────────────────────────────────────────────────────────────

  /**
   * Récupère les statistiques du Centre de Thérapie Cellulaire depuis le ctc-service.
   * Endpoint : GET /api/v1/ctc/dashboard
   * En cas d'erreur réseau, retourne des valeurs de repli à zéro.
   *
   * @returns Observable<CtcStatsSummary>
   */
  getCtcStats(): Observable<CtcStatsSummary> {
    return this.http.get<CtcStatsSummary>(`${this.apiBaseUrl}/ctc/dashboard`).pipe(
      catchError(() => of<CtcStatsSummary>({
        activeDossiers:      0,
        pendingManufacturing: 0,
      }))
    );
  }

  // ─────────────────────────────────────────────────────────────
  // Statistiques Pharmacie — données réelles via pharmacy-service
  // ─────────────────────────────────────────────────────────────

  /**
   * Récupère les statistiques de la Pharmacie depuis le pharmacy-service.
   * Endpoint : GET /api/v1/pharmacy/dashboard
   * En cas d'erreur réseau, retourne des valeurs de repli à zéro.
   *
   * @returns Observable<PharmacyStatsSummary>
   */
  getPharmacyStats(): Observable<PharmacyStatsSummary> {
    return this.http.get<PharmacyStatsSummary>(`${this.apiBaseUrl}/pharmacy/dashboard`).pipe(
      catchError(() => of<PharmacyStatsSummary>({
        dispensationsToday: 0,
        criticalStock:      0,
      }))
    );
  }

  // ─────────────────────────────────────────────────────────────
  // Tâches urgentes — mock avec deadlines proches
  // ─────────────────────────────────────────────────────────────

  /**
   * Retourne la liste des tâches nécessitant une action imminente.
   * Les dates sont relatives à aujourd'hui pour garantir la pertinence.
   *
   * @returns UrgentTask[]
   */
  getUrgentTasks(): UrgentTask[] {
    const today = new Date();
    const addDays = (d: Date, n: number): Date => {
      const r = new Date(d);
      r.setDate(r.getDate() + n);
      return r;
    };

    return [
      {
        id:         'task-001',
        title:      'Approbation amendement — CARDIO-2024-07',
        module:     'Comité d\'Éthique',
        moduleIcon: 'pi-shield',
        deadline:   addDays(today, 2),
        priority:   'high',
        route:      '/ethics',
        type:       'approval',
      },
      {
        id:         'task-002',
        title:      'Dispensation chimiothérapie — Dossier CTC-114',
        module:     'Pharmacie',
        moduleIcon: 'pi-box',
        deadline:   addDays(today, 0),
        priority:   'high',
        route:      '/pharmacy',
        type:       'dispensation',
      },
      {
        id:         'task-003',
        title:      'Rapport annuel ONCO-PHASE3-2023 à soumettre',
        module:     'Études cliniques',
        moduleIcon: 'pi-book',
        deadline:   addDays(today, 5),
        priority:   'medium',
        route:      '/studies',
        type:       'submission',
      },
      {
        id:         'task-004',
        title:      'Validation fabrication CAR-T — Patient P-0892',
        module:     'CTC',
        moduleIcon: 'pi-sitemap',
        deadline:   addDays(today, 3),
        priority:   'high',
        route:      '/ctc',
        type:       'review',
      },
      {
        id:         'task-005',
        title:      'Renouvellement autorisation CE — NEURO-2024-02',
        module:     'Comité d\'Éthique',
        moduleIcon: 'pi-shield',
        deadline:   addDays(today, 6),
        priority:   'medium',
        route:      '/ethics',
        type:       'approval',
      },
    ];
  }

  // ─────────────────────────────────────────────────────────────
  // Timeline des échéances — mock réaliste
  // ─────────────────────────────────────────────────────────────

  /**
   * Retourne la timeline des prochaines échéances cliniques.
   * 7 items couvrant les 60 prochains jours.
   *
   * @returns TimelineItem[]
   */
  getTimeline(): TimelineItem[] {
    const today = new Date();
    const addDays = (d: Date, n: number): Date => {
      const r = new Date(d);
      r.setDate(r.getDate() + n);
      return r;
    };

    const items: TimelineItem[] = [
      {
        id:       'tl-001',
        label:    'Clôture inclusions — CARDIO-2024-07',
        date:     addDays(today, 2),
        icon:     'pi-users',
        color:    '#1565c0',
        module:   'Études cliniques',
        route:    '/studies',
        isUrgent: true,
      },
      {
        id:       'tl-002',
        label:    'Comité CE mensuel — Session juin 2026',
        date:     addDays(today, 4),
        icon:     'pi-shield',
        color:    '#8b5cf6',
        module:   'Comité d\'Éthique',
        route:    '/ethics',
        isUrgent: true,
      },
      {
        id:       'tl-003',
        label:    'Libération lot CAR-T — CTC-114',
        date:     addDays(today, 7),
        icon:     'pi-sitemap',
        color:    '#f59e0b',
        module:   'CTC',
        route:    '/ctc',
        isUrgent: false,
      },
      {
        id:       'tl-004',
        label:    'Inventaire trimestriel — Médicaments essai',
        date:     addDays(today, 14),
        icon:     'pi-box',
        color:    '#10b981',
        module:   'Pharmacie',
        route:    '/pharmacy',
        isUrgent: false,
      },
      {
        id:       'tl-005',
        label:    'Soumission rapport EUDRACT — ONCO-PHASE3',
        date:     addDays(today, 21),
        icon:     'pi-send',
        color:    '#1565c0',
        module:   'Études cliniques',
        route:    '/studies',
        isUrgent: false,
      },
      {
        id:       'tl-006',
        label:    'Audit interne qualité — GED Documents',
        date:     addDays(today, 35),
        icon:     'pi-folder-open',
        color:    '#64748b',
        module:   'Documents',
        route:    '/documents',
        isUrgent: false,
      },
      {
        id:       'tl-007',
        label:    'Renouvellement annuel assurance essai — NEURO-02',
        date:     addDays(today, 58),
        icon:     'pi-file-edit',
        color:    '#ef4444',
        module:   'Administration',
        route:    '/admin',
        isUrgent: false,
      },
    ];

    return items.sort((a, b) => a.date.getTime() - b.date.getTime());
  }
}
