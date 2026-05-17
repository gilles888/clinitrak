import {
  Component,
  computed,
  inject,
  OnDestroy,
  OnInit,
  signal,
} from '@angular/core';
import { RouterLink }    from '@angular/router';
import { DatePipe, NgStyle } from '@angular/common';
import { ButtonModule }   from 'primeng/button';
import { BadgeModule }    from 'primeng/badge';
import { TagModule }      from 'primeng/tag';
import { TooltipModule }  from 'primeng/tooltip';
import { RippleModule }   from 'primeng/ripple';
import { SkeletonModule } from 'primeng/skeleton';
import { AvatarModule }   from 'primeng/avatar';
import { ChipModule }     from 'primeng/chip';

import { currentUser, hasAnyRole } from '../../core/store/auth.store';
import { SystemRole }               from '../../core/models/user.model';
import { DashboardService }         from './dashboard.service';
import {
  DashboardCard,
  DashboardStats,
  StudyStatsSummary,
  TimelineItem,
  UrgentTask,
} from './dashboard.models';

/**
 * Tableau de bord principal de CliniTrak.
 *
 * <p>Affiche les KPI en temps réel, les cards modules (filtrées par RBAC),
 * les tâches urgentes et la timeline des échéances cliniques.
 * Supporte le raccourci clavier Ctrl+K pour la recherche globale.
 *
 * @example
 * Accessible via la route `/dashboard` (lazy-loaded depuis app.routes.ts).
 */
@Component({
  selector:    'app-dashboard',
  standalone:  true,
  templateUrl: './dashboard.component.html',
  styleUrl:    './dashboard.component.scss',
  imports: [
    DatePipe,
    NgStyle,
    RouterLink,
    ButtonModule,
    BadgeModule,
    TagModule,
    TooltipModule,
    RippleModule,
    SkeletonModule,
    AvatarModule,
    ChipModule,
  ],
})
export class DashboardComponent implements OnInit, OnDestroy {

  private readonly dashboardService = inject(DashboardService);

  // ─── Store signals ────────────────────────────────────────────
  /** Utilisateur authentifié courant (lecture seule depuis le store). */
  protected readonly currentUser = currentUser;

  // ─── Constantes ───────────────────────────────────────────────
  /** Date courante exposée pour le pipe date dans le template. */
  protected readonly today = new Date();

  // ─── État local ───────────────────────────────────────────────
  /** Indicateur de chargement global. */
  protected readonly isLoading = signal(true);
  /** Statistiques agrégées de tous les modules. */
  protected readonly dashboardStats = signal<DashboardStats | null>(null);
  /** Tâches urgentes à traiter. */
  protected readonly urgentTasks = signal<UrgentTask[]>([]);
  /** Échéances de la timeline. */
  protected readonly timeline = signal<TimelineItem[]>([]);
  /** Index de la card survolée (-1 si aucune). */
  protected readonly activeCardIndex = signal<number>(-1);
  /** Requête de recherche globale. */
  protected readonly searchQuery = signal('');
  /** Visibilité de la barre de recherche globale. */
  protected readonly showGlobalSearch = signal(false);

  /** Valeurs animées des KPI (counter up). */
  protected readonly animatedStudies     = signal(0);
  protected readonly animatedPending     = signal(0);
  protected readonly animatedCtcDossiers = signal(0);
  protected readonly animatedDispToday   = signal(0);

  private readonly keydownListener = (e: KeyboardEvent) => this.handleKeydown(e);

  // ─── Computed signals ─────────────────────────────────────────

  /**
   * Heure-dépendant : salutation personnalisée.
   */
  protected readonly greeting = computed<string>(() => {
    const h = new Date().getHours();
    if (h < 12) return 'Bonjour';
    if (h < 18) return 'Bon après-midi';
    return 'Bonsoir';
  });

  /**
   * Nombre de tâches accessibles dont la deadline est dans moins de 7 jours.
   */
  protected readonly urgentTasksCount = computed<number>(() =>
    this.filteredUrgentTasks().filter(t => {
      const diff = t.deadline.getTime() - Date.now();
      return diff < 7 * 24 * 3600 * 1000;
    }).length
  );

  /**
   * Statistiques études extraites du store global.
   */
  protected readonly studyStats = computed<StudyStatsSummary | null>(() =>
    this.dashboardStats()?.studyStats ?? null
  );

  /**
   * Définitions des cards modules — filtrées selon les rôles RBAC.
   */
  protected readonly visibleCards = computed<DashboardCard[]>(() => {
    const user = this.currentUser();
    if (!user) return [];

    const userRoles = new Set(user.roles);

    return ALL_MODULE_CARDS.filter(card => {
      if (!card.requiredRoles || card.requiredRoles.length === 0) return true;
      return card.requiredRoles.some(r => userRoles.has(r));
    });
  });

  /**
   * Routes de base accessibles à l'utilisateur (ex: "/ethics", "/ctc").
   * Dérivé de visibleCards — sert à filtrer tâches et timeline.
   */
  private readonly allowedRoutes = computed<Set<string>>(() =>
    new Set(this.visibleCards().map(c => c.route))
  );

  /**
   * Tâches urgentes filtrées selon les modules accessibles (RBAC).
   */
  protected readonly filteredUrgentTasks = computed<UrgentTask[]>(() => {
    const allowed = this.allowedRoutes();
    return this.urgentTasks().filter(t => {
      const base = '/' + t.route.split('/')[1];
      return allowed.has(base);
    });
  });

  /**
   * Timeline filtrée selon les modules accessibles (RBAC).
   */
  protected readonly filteredTimeline = computed<TimelineItem[]>(() => {
    const allowed = this.allowedRoutes();
    return this.timeline().filter(item => {
      const base = '/' + item.route.split('/')[1];
      return allowed.has(base);
    });
  });

  // ─────────────────────────────────────────────────────────────
  // Cycle de vie
  // ─────────────────────────────────────────────────────────────

  ngOnInit(): void {
    document.addEventListener('keydown', this.keydownListener);
    this.loadDashboard();
  }

  ngOnDestroy(): void {
    document.removeEventListener('keydown', this.keydownListener);
  }

  // ─────────────────────────────────────────────────────────────
  // Chargement des données
  // ─────────────────────────────────────────────────────────────

  /** Charge toutes les données du tableau de bord depuis {@link DashboardService}. */
  private loadDashboard(): void {
    this.isLoading.set(true);
    this.dashboardService.loadAll().subscribe({
      next: data => {
        this.dashboardStats.set(data.stats);
        this.urgentTasks.set(data.urgentTasks);
        this.timeline.set(data.timeline);
        this.isLoading.set(false);
        this.animateCounters(
          data.stats.studyStats.active,
          data.stats.ethicsStats.pendingSubmissions,
          data.stats.ctcStats.activeDossiers,
          data.stats.pharmacyStats.dispensationsToday,
        );
      },
      error: err => {
        console.error('[DashboardComponent] Erreur chargement dashboard', err);
        this.isLoading.set(false);
      },
    });
  }

  // ─────────────────────────────────────────────────────────────
  // Animations compteurs KPI
  // ─────────────────────────────────────────────────────────────

  /**
   * Anime les compteurs KPI avec requestAnimationFrame (sans lib externe).
   *
   * @param studies   nombre d'études actives
   * @param pending   soumissions CE en attente
   * @param ctc       dossiers CTC actifs
   * @param dispToday dispensations du jour
   */
  private animateCounters(
    studies: number,
    pending: number,
    ctc: number,
    dispToday: number,
  ): void {
    const duration = 900;
    const fps = 60;
    const steps = Math.round((duration / 1000) * fps);

    const targets: Array<{ signal: ReturnType<typeof signal<number>>; target: number }> = [
      { signal: this.animatedStudies,     target: studies    },
      { signal: this.animatedPending,     target: pending    },
      { signal: this.animatedCtcDossiers, target: ctc        },
      { signal: this.animatedDispToday,   target: dispToday  },
    ];

    let step = 0;

    const tick = () => {
      step++;
      const progress = this.easeOut(step / steps);
      for (const item of targets) {
        item.signal.set(Math.round(item.target * progress));
      }
      if (step < steps) {
        requestAnimationFrame(tick);
      } else {
        for (const item of targets) {
          item.signal.set(item.target);
        }
      }
    };

    requestAnimationFrame(tick);
  }

  /** Fonction easing pour les animations compteurs. */
  private easeOut(t: number): number {
    return 1 - Math.pow(1 - t, 3);
  }

  // ─────────────────────────────────────────────────────────────
  // Raccourcis clavier
  // ─────────────────────────────────────────────────────────────

  /**
   * Gère les raccourcis clavier globaux.
   * Ctrl+K → toggle recherche. Escape → ferme recherche.
   *
   * @param e événement clavier
   */
  private handleKeydown(e: KeyboardEvent): void {
    if ((e.ctrlKey || e.metaKey) && e.key === 'k') {
      e.preventDefault();
      this.showGlobalSearch.update(v => !v);
    }
    if (e.key === 'Escape' && this.showGlobalSearch()) {
      this.showGlobalSearch.set(false);
      this.searchQuery.set('');
    }
  }

  // ─────────────────────────────────────────────────────────────
  // Helpers template
  // ─────────────────────────────────────────────────────────────

  /**
   * Calcule le nombre de jours restants avant une deadline.
   *
   * @param deadline date limite
   * @returns nombre de jours (négatif si dépassée)
   */
  protected daysUntil(deadline: Date): number {
    const ms = deadline.getTime() - Date.now();
    return Math.ceil(ms / (1000 * 3600 * 24));
  }

  /**
   * Libellé humain pour le temps restant avant une échéance.
   *
   * @param deadline date limite
   * @returns libellé affiché (ex: 'Aujourd\'hui', 'Demain', 'Dans 3 jours', 'En retard')
   */
  protected deadlineLabel(deadline: Date): string {
    const d = this.daysUntil(deadline);
    if (d < 0)  return 'En retard';
    if (d === 0) return 'Aujourd\'hui';
    if (d === 1) return 'Demain';
    return `Dans ${d} jours`;
  }

  /**
   * Classe CSS de priorité pour les badges tâches.
   *
   * @param priority priorité de la tâche
   * @returns nom de la classe CSS
   */
  protected priorityClass(priority: string): string {
    switch (priority) {
      case 'high':   return 'priority-high';
      case 'medium': return 'priority-medium';
      default:       return 'priority-low';
    }
  }

  /**
   * Couleur du badge deadline selon l'urgence.
   *
   * @param deadline date limite
   * @returns classe CSS
   */
  protected deadlineClass(deadline: Date): string {
    const d = this.daysUntil(deadline);
    if (d < 0)  return 'deadline-overdue';
    if (d <= 1) return 'deadline-critical';
    if (d <= 3) return 'deadline-warning';
    return 'deadline-ok';
  }

  /** Met à jour la query de recherche. */
  protected onSearchInput(event: Event): void {
    this.searchQuery.set((event.target as HTMLInputElement).value);
  }

  /** Ferme la recherche globale. */
  protected closeSearch(): void {
    this.showGlobalSearch.set(false);
    this.searchQuery.set('');
  }

  /**
   * Retourne la couleur hex associée à une icône de module.
   * Utilisé pour colorer l'icône dans le widget des tâches urgentes.
   *
   * @param moduleIcon icône PrimeIcons du module
   * @returns couleur hex
   */
  protected getModuleColor(moduleIcon: string): string {
    const map: Record<string, string> = {
      'pi-book':        '#1565c0',
      'pi-shield':      '#8b5cf6',
      'pi-sitemap':     '#f59e0b',
      'pi-box':         '#10b981',
      'pi-globe':       '#06b6d4',
      'pi-folder-open': '#64748b',
      'pi-euro':        '#f97316',
      'pi-cog':         '#ef4444',
    };
    return map[moduleIcon] ?? '#64748b';
  }
}

// ─────────────────────────────────────────────────────────────
// Définitions statiques des cards modules
// ─────────────────────────────────────────────────────────────

/**
 * Toutes les cards modules de la plateforme.
 * Filtrées dynamiquement dans `visibleCards` selon les rôles RBAC.
 */
const ALL_MODULE_CARDS: DashboardCard[] = [
  {
    id:         'studies',
    title:      'Études cliniques',
    subtitle:   'Gestion du portefeuille d\'études',
    route:      '/studies',
    icon:       'pi-book',
    color:      '#1565c0',
    colorLight: '#1565c020',
    value:      '—',
    trend:      'Portefeuille actif',
    trendUp:    true,
    actions: [
      { label: 'Nouvelle étude', icon: 'pi-plus',   route: '/studies/new' },
      { label: 'Rechercher',     icon: 'pi-search', route: '/studies'     },
    ],
  },
  {
    id:         'ethics',
    title:      'Comité d\'Éthique',
    subtitle:   'Soumissions et avis CE',
    route:      '/ethics',
    icon:       'pi-shield',
    color:      '#8b5cf6',
    colorLight: '#8b5cf620',
    value:      '—',
    trend:      '4 dossiers en cours',
    trendUp:    true,
    requiredRoles: [
      SystemRole.CE_SECRETARY,
      SystemRole.CE_COORDINATOR,
      SystemRole.ADMIN_TENANT,
      SystemRole.SUPER_ADMIN,
    ],
    actions: [
      { label: 'Nouvelle soumission', icon: 'pi-plus',   route: '/ethics/new' },
      { label: 'Calendrier CE',       icon: 'pi-calendar', route: '/ethics'     },
    ],
  },
  {
    id:         'ctc',
    title:      'CTC',
    subtitle:   'Centre de Thérapie Cellulaire',
    route:      '/ctc',
    icon:       'pi-sitemap',
    color:      '#f59e0b',
    colorLight: '#f59e0b20',
    value:      '—',
    trend:      '11 dossiers actifs',
    trendUp:    true,
    requiredRoles: [
      SystemRole.CTC_DESK,
      SystemRole.CTC_CRA,
      SystemRole.CTC_PM,
      SystemRole.CTC_COFI,
      SystemRole.ADMIN_TENANT,
      SystemRole.SUPER_ADMIN,
    ],
    actions: [
      { label: 'Nouveau dossier', icon: 'pi-plus',     route: '/ctc/new'  },
      { label: 'Tableau de bord', icon: 'pi-chart-bar', route: '/ctc'      },
    ],
  },
  {
    id:         'pharmacy',
    title:      'Pharmacie',
    subtitle:   'Médicaments expérimentaux',
    route:      '/pharmacy',
    icon:       'pi-box',
    color:      '#10b981',
    colorLight: '#10b98120',
    value:      '—',
    trend:      '7 dispensations / jour',
    trendUp:    true,
    requiredRoles: [
      SystemRole.PHARMACIST,
      SystemRole.ADMIN_TENANT,
      SystemRole.SUPER_ADMIN,
    ],
    actions: [
      { label: 'Nouvelle dispensation', icon: 'pi-plus',      route: '/pharmacy/dispense' },
      { label: 'Stock',                  icon: 'pi-warehouse', route: '/pharmacy'          },
    ],
  },
  {
    id:         'exchange',
    title:      'Échanges externes',
    subtitle:   'Portail partenaires et CRO',
    route:      '/exchange',
    icon:       'pi-globe',
    color:      '#06b6d4',
    colorLight: '#06b6d420',
    value:      '—',
    trend:      'Demandes actives',
    trendUp:    true,
    actions: [
      { label: 'Nouvelle demande', icon: 'pi-plus',      route: '/exchange/new' },
      { label: 'Messagerie',       icon: 'pi-envelope', route: '/exchange'      },
    ],
  },
  {
    id:         'documents',
    title:      'Documents',
    subtitle:   'GED — archivage sécurisé',
    route:      '/documents',
    icon:       'pi-folder-open',
    color:      '#64748b',
    colorLight: '#64748b20',
    value:      '—',
    trend:      'MinIO chiffré',
    trendUp:    true,
    actions: [
      { label: 'Déposer',     icon: 'pi-upload', route: '/documents/upload' },
      { label: 'Rechercher',  icon: 'pi-search', route: '/documents'         },
    ],
  },
  {
    id:         'billing',
    title:      'Facturation',
    subtitle:   'Facturation et conventions',
    route:      '/billing',
    icon:       'pi-euro',
    color:      '#f97316',
    colorLight: '#f9731620',
    value:      '—',
    trend:      'Gestion financière',
    trendUp:    true,
    requiredRoles: [
      SystemRole.CTC_COFI,
      SystemRole.ADMIN_TENANT,
      SystemRole.SUPER_ADMIN,
    ],
    actions: [
      { label: 'Nouvelle facture',  icon: 'pi-plus',     route: '/billing/new' },
      { label: 'Tableau de bord',   icon: 'pi-chart-bar', route: '/billing'     },
    ],
  },
  {
    id:         'admin',
    title:      'Administration',
    subtitle:   'Gestion des utilisateurs et tenant',
    route:      '/admin',
    icon:       'pi-cog',
    color:      '#ef4444',
    colorLight: '#ef444420',
    value:      '—',
    trend:      'Configuration système',
    trendUp:    true,
    requiredRoles: [
      SystemRole.ADMIN_TENANT,
      SystemRole.SUPER_ADMIN,
    ],
    actions: [
      { label: 'Utilisateurs', icon: 'pi-users', route: '/admin/users'  },
      { label: 'Audit logs',   icon: 'pi-list',  route: '/admin/audit'  },
    ],
  },
];
