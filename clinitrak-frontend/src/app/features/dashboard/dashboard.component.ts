import { Component, computed, inject, OnInit, signal } from '@angular/core';
import { RouterLink } from '@angular/router';
import { CardModule } from 'primeng/card';
import { TagModule } from 'primeng/tag';
import { currentUser, hasAnyRole } from '../../core/store/auth.store';
import { SystemRole } from '../../core/models/user.model';
import { StudyService } from '../../core/services/study.service';
import { StudyStatisticsResponse } from '../../core/models/study.model';

/**
 * Tableau de bord personnalisé par rôle.
 *
 * <p>Les widgets affichés varient en fonction des rôles de l'utilisateur connecté.
 * Les statistiques des études sont chargées en temps réel depuis {@link StudyService#getStatistics}.
 * Les autres métriques (CE, CTC, Documents) sont des placeholders en attente de leurs services.
 */
@Component({
  selector: 'app-dashboard',
  standalone: true,
  imports: [CardModule, TagModule, RouterLink],
  template: `
    <div class="tw-space-y-6">

      <!-- Titre de bienvenue -->
      <div>
        <h2 class="tw-text-2xl tw-font-bold tw-text-gray-900">
          Bonjour, {{ currentUser()?.firstName }} 👋
        </h2>
        <p class="tw-text-gray-500 tw-mt-1">
          Voici un aperçu de l'activité de la plateforme CliniTrak.
        </p>
      </div>

      <!-- Statistiques rapides -->
      <div class="tw-grid tw-grid-cols-1 sm:tw-grid-cols-2 lg:tw-grid-cols-4 tw-gap-4">

        @for (stat of stats(); track stat.label) {
          <p-card styleClass="tw-border tw-border-gray-100 tw-shadow-sm hover:tw-shadow-md tw-transition-shadow">
            <div class="tw-flex tw-items-center tw-justify-between">
              <div>
                <p class="tw-text-sm tw-text-gray-500">{{ stat.label }}</p>
                @if (isLoadingStats()) {
                  <p class="tw-text-3xl tw-font-bold tw-text-gray-300 tw-mt-1">
                    <i class="pi pi-spin pi-spinner tw-text-2xl"></i>
                  </p>
                } @else {
                  <p class="tw-text-3xl tw-font-bold tw-text-gray-900 tw-mt-1">{{ stat.value }}</p>
                }
                <p class="tw-text-xs tw-mt-1" [class]="stat.trendPositive ? 'tw-text-green-600' : 'tw-text-gray-400'">
                  {{ stat.trend }}
                </p>
              </div>
              <div class="tw-w-12 tw-h-12 tw-rounded-xl tw-flex tw-items-center tw-justify-center"
                   [style.background]="stat.color + '20'">
                <i [class]="'pi ' + stat.icon + ' tw-text-2xl'" [style.color]="stat.color"></i>
              </div>
            </div>
          </p-card>
        }
      </div>

      <!-- Détails études cliniques -->
      @if (!isLoadingStats() && studyStats()) {
        <div class="tw-grid tw-grid-cols-1 lg:tw-grid-cols-2 tw-gap-6">

          <!-- Études par statut -->
          <p-card header="Études par statut" styleClass="tw-border tw-border-gray-100">
            <div class="tw-space-y-3">
              <div class="tw-flex tw-items-center tw-justify-between">
                <div class="tw-flex tw-items-center tw-gap-2">
                  <span class="tw-w-3 tw-h-3 tw-rounded-full tw-bg-gray-300 tw-inline-block"></span>
                  <span class="tw-text-sm tw-text-gray-600">Brouillon</span>
                </div>
                <span class="tw-font-semibold tw-text-gray-900">{{ studyStats()!.draftStudies }}</span>
              </div>
              <div class="tw-flex tw-items-center tw-justify-between">
                <div class="tw-flex tw-items-center tw-gap-2">
                  <span class="tw-w-3 tw-h-3 tw-rounded-full tw-bg-green-500 tw-inline-block"></span>
                  <span class="tw-text-sm tw-text-gray-600">En cours</span>
                </div>
                <span class="tw-font-semibold tw-text-gray-900">{{ studyStats()!.ongoingStudies }}</span>
              </div>
              <div class="tw-flex tw-items-center tw-justify-between">
                <div class="tw-flex tw-items-center tw-gap-2">
                  <span class="tw-w-3 tw-h-3 tw-rounded-full tw-bg-blue-500 tw-inline-block"></span>
                  <span class="tw-text-sm tw-text-gray-600">Approuvées</span>
                </div>
                <span class="tw-font-semibold tw-text-gray-900">{{ studyStats()!.approvedStudies }}</span>
              </div>
              <div class="tw-flex tw-items-center tw-justify-between">
                <div class="tw-flex tw-items-center tw-gap-2">
                  <span class="tw-w-3 tw-h-3 tw-rounded-full tw-bg-gray-500 tw-inline-block"></span>
                  <span class="tw-text-sm tw-text-gray-600">Clôturées</span>
                </div>
                <span class="tw-font-semibold tw-text-gray-900">{{ studyStats()!.closedStudies }}</span>
              </div>
            </div>
            <div class="tw-mt-4 tw-pt-4 tw-border-t tw-border-gray-100">
              <a routerLink="/studies" class="tw-text-sm tw-text-blue-600 hover:tw-underline tw-flex tw-items-center tw-gap-1">
                <i class="pi pi-arrow-right tw-text-xs"></i>
                Voir toutes les études
              </a>
            </div>
          </p-card>

          <!-- Sponsor CUSL -->
          <p-card header="Répartition sponsor" styleClass="tw-border tw-border-gray-100">
            <div class="tw-space-y-4">
              <div class="tw-flex tw-justify-between tw-items-center">
                <span class="tw-text-sm tw-text-gray-600">Études sponsorisées CUSL</span>
                <p-tag [value]="studyStats()!.sponsorCuslStudies.toString()" severity="success" />
              </div>
              <div class="tw-flex tw-justify-between tw-items-center">
                <span class="tw-text-sm tw-text-gray-600">Total études</span>
                <p-tag [value]="studyStats()!.totalStudies.toString()" severity="info" />
              </div>
            </div>
          </p-card>

        </div>
      }

      <!-- Activité récente -->
      <p-card header="Activité récente" styleClass="tw-border tw-border-gray-100">
        <p class="tw-text-gray-500 tw-text-sm">
          L'historique d'activité sera disponible dans une prochaine version.
        </p>
      </p-card>
    </div>
  `,
})
export class DashboardComponent implements OnInit {

  private readonly studyService = inject(StudyService);

  protected readonly currentUser = currentUser;

  /** Statistiques brutes reçues depuis l'API study-service. */
  protected readonly studyStats      = signal<StudyStatisticsResponse | null>(null);
  /** Indicateur de chargement des statistiques. */
  protected readonly isLoadingStats  = signal(false);

  ngOnInit(): void {
    this.loadStats();
  }

  /** Charge les statistiques depuis le study-service. */
  private loadStats(): void {
    this.isLoadingStats.set(true);
    this.studyService.getStatistics().subscribe({
      next: (stats) => {
        this.studyStats.set(stats);
        this.isLoadingStats.set(false);
      },
      error: (err) => {
        console.error('Erreur chargement statistiques', err);
        this.isLoadingStats.set(false);
      },
    });
  }

  /**
   * Statistiques du tableau de bord calculées depuis les données réelles.
   * Les métriques CE, CTC et Documents sont en attente de leurs services respectifs.
   */
  protected readonly stats = computed(() => {
    const s = this.studyStats();
    return [
      {
        label:         'Études actives',
        value:         s ? String(s.ongoingStudies + s.approvedStudies) : '—',
        icon:          'pi-book',
        color:         '#3b82f6',
        trend:         s ? `${s.totalStudies} études au total` : 'Chargement...',
        trendPositive: true,
      },
      {
        label:         'Soumissions CE',
        value:         '—',
        icon:          'pi-shield',
        color:         '#8b5cf6',
        trend:         'Module ethics en développement',
        trendPositive: false,
      },
      {
        label:         'Dossiers CTC',
        value:         '—',
        icon:          'pi-sitemap',
        color:         '#f59e0b',
        trend:         'Module CTC en développement',
        trendPositive: false,
      },
      {
        label:         'Documents',
        value:         '—',
        icon:          'pi-folder',
        color:         '#10b981',
        trend:         'Module documents en développement',
        trendPositive: false,
      },
    ];
  });
}
