import { Component, inject, OnInit, signal } from '@angular/core';
import { CommonModule, DatePipe } from '@angular/common';
import { Router, RouterLink } from '@angular/router';
import { ButtonModule } from 'primeng/button';
import { CardModule } from 'primeng/card';
import { MessageModule } from 'primeng/message';
import { TableModule } from 'primeng/table';
import { TagModule } from 'primeng/tag';
import { EthicsService } from '../../../core/services/ethics.service';
import {
  DECISION_SEVERITY,
  MEETING_STATUS_SEVERITY,
  EthicsDashboardResponse,
} from '../../../core/models/ethics.model';

/**
 * Composant tableau de bord du Comité d'Éthique.
 *
 * <p>Affiche les indicateurs clés (KPIs), les derniers avis en attente,
 * la prochaine réunion planifiée et une alerte si des rapports annuels
 * sont en retard.
 *
 * <p>Les données sont récupérées via {@link EthicsService#getDashboard} au moment
 * de l'initialisation du composant.
 */
@Component({
  selector: 'app-ethics-dashboard',
  standalone: true,
  imports: [
    CommonModule,
    DatePipe,
    RouterLink,
    ButtonModule,
    CardModule,
    MessageModule,
    TableModule,
    TagModule,
  ],
  template: `
    <div class="tw-space-y-6">

      <!-- En-tête -->
      <div class="tw-flex tw-items-center tw-justify-between">
        <div>
          <h2 class="tw-text-2xl tw-font-bold tw-text-gray-900">Comité d'Éthique</h2>
          <p class="tw-text-sm tw-text-gray-500 tw-mt-1">Vue d'ensemble et indicateurs clés</p>
        </div>
        <p-button
          label="Nouvel avis CE"
          icon="pi pi-plus"
          routerLink="/ethics/reviews/new"
          severity="primary"
        />
      </div>

      <!-- Chargement -->
      @if (isLoading()) {
        <div class="tw-flex tw-justify-center tw-py-12">
          <i class="pi pi-spin pi-spinner tw-text-4xl tw-text-blue-500"></i>
        </div>
      }

      @if (!isLoading() && dashboard()) {
        <!-- KPI Cards -->
        <div class="tw-grid tw-grid-cols-1 sm:tw-grid-cols-2 xl:tw-grid-cols-4 tw-gap-4">

          <!-- Avis en attente -->
          <div class="tw-bg-white tw-rounded-xl tw-border tw-border-gray-200 tw-p-5 tw-flex tw-items-center tw-gap-4 tw-shadow-sm">
            <div class="tw-w-12 tw-h-12 tw-bg-orange-100 tw-rounded-full tw-flex tw-items-center tw-justify-center tw-shrink-0">
              <i class="pi pi-clock tw-text-orange-500 tw-text-xl"></i>
            </div>
            <div>
              <p class="tw-text-3xl tw-font-bold tw-text-orange-600">{{ dashboard()!.pendingReviews }}</p>
              <p class="tw-text-sm tw-text-gray-600 tw-mt-0.5">Avis en attente</p>
            </div>
          </div>

          <!-- Soumissions ce mois -->
          <div class="tw-bg-white tw-rounded-xl tw-border tw-border-gray-200 tw-p-5 tw-flex tw-items-center tw-gap-4 tw-shadow-sm">
            <div class="tw-w-12 tw-h-12 tw-bg-blue-100 tw-rounded-full tw-flex tw-items-center tw-justify-center tw-shrink-0">
              <i class="pi pi-calendar tw-text-blue-500 tw-text-xl"></i>
            </div>
            <div>
              <p class="tw-text-3xl tw-font-bold tw-text-blue-600">{{ dashboard()!.reviewsLastMonth }}</p>
              <p class="tw-text-sm tw-text-gray-600 tw-mt-0.5">Soumissions ce mois</p>
            </div>
          </div>

          <!-- Rapports annuels dus -->
          <div class="tw-bg-white tw-rounded-xl tw-border tw-border-gray-200 tw-p-5 tw-flex tw-items-center tw-gap-4 tw-shadow-sm">
            <div class="tw-w-12 tw-h-12 tw-rounded-full tw-flex tw-items-center tw-justify-center tw-shrink-0"
                 [class]="dashboard()!.annualReportsDue > 0 ? 'tw-bg-red-100' : 'tw-bg-gray-100'">
              <i class="pi pi-file tw-text-xl"
                 [class]="dashboard()!.annualReportsDue > 0 ? 'tw-text-red-500' : 'tw-text-gray-400'"></i>
            </div>
            <div>
              <p class="tw-text-3xl tw-font-bold"
                 [class]="dashboard()!.annualReportsDue > 0 ? 'tw-text-red-600' : 'tw-text-gray-700'">
                {{ dashboard()!.annualReportsDue }}
              </p>
              <p class="tw-text-sm tw-text-gray-600 tw-mt-0.5">Rapports annuels dus</p>
            </div>
          </div>

          <!-- Prochaines réunions -->
          <div class="tw-bg-white tw-rounded-xl tw-border tw-border-gray-200 tw-p-5 tw-flex tw-items-center tw-gap-4 tw-shadow-sm">
            <div class="tw-w-12 tw-h-12 tw-bg-green-100 tw-rounded-full tw-flex tw-items-center tw-justify-center tw-shrink-0">
              <i class="pi pi-users tw-text-green-500 tw-text-xl"></i>
            </div>
            <div>
              <p class="tw-text-3xl tw-font-bold tw-text-green-600">{{ dashboard()!.upcomingMeetings }}</p>
              <p class="tw-text-sm tw-text-gray-600 tw-mt-0.5">Prochaines réunions</p>
            </div>
          </div>
        </div>

        <!-- Alerte rapports annuels en retard -->
        @if (dashboard()!.annualReportsOverdue > 0) {
          <div class="tw-bg-red-50 tw-border tw-border-red-200 tw-rounded-lg tw-p-4 tw-flex tw-items-center tw-justify-between">
            <div class="tw-flex tw-items-center tw-gap-3">
              <i class="pi pi-exclamation-triangle tw-text-red-500 tw-text-xl"></i>
              <span class="tw-text-red-700 tw-font-medium">
                {{ dashboard()!.annualReportsOverdue }} rapport(s) annuel(s) en retard
              </span>
            </div>
            <a routerLink="/ethics/annual-reports"
               class="tw-text-red-600 tw-underline tw-text-sm tw-font-semibold hover:tw-text-red-800">
              Voir les rapports
            </a>
          </div>
        }

        <!-- Grille 2 colonnes : derniers avis + prochaine réunion -->
        <div class="tw-grid tw-grid-cols-1 lg:tw-grid-cols-2 tw-gap-6">

          <!-- Derniers avis en attente -->
          <div class="tw-bg-white tw-rounded-xl tw-border tw-border-gray-200 tw-shadow-sm">
            <div class="tw-flex tw-items-center tw-justify-between tw-px-5 tw-py-4 tw-border-b tw-border-gray-100">
              <h3 class="tw-font-semibold tw-text-gray-800">Derniers avis en attente</h3>
              <a routerLink="/ethics/reviews"
                 class="tw-text-blue-600 tw-text-sm hover:tw-underline">Voir tout</a>
            </div>

            @if (dashboard()!.recentPendingReviews.length === 0) {
              <div class="tw-py-10 tw-text-center tw-text-gray-400">
                <i class="pi pi-check-circle tw-text-3xl tw-block tw-mb-2"></i>
                <p class="tw-text-sm">Aucun avis en attente</p>
              </div>
            } @else {
              <p-table
                [value]="dashboard()!.recentPendingReviews"
                [scrollable]="true"
                styleClass="tw-text-sm"
              >
                <ng-template pTemplate="header">
                  <tr class="tw-bg-gray-50">
                    <th class="tw-font-semibold tw-text-xs tw-text-gray-600 tw-px-3 tw-py-2">N° CE</th>
                    <th class="tw-font-semibold tw-text-xs tw-text-gray-600 tw-px-3 tw-py-2">Étude</th>
                    <th class="tw-font-semibold tw-text-xs tw-text-gray-600 tw-px-3 tw-py-2">Type</th>
                    <th class="tw-font-semibold tw-text-xs tw-text-gray-600 tw-px-3 tw-py-2">Soumission</th>
                    <th class="tw-font-semibold tw-text-xs tw-text-gray-600 tw-px-3 tw-py-2">Rapporteur</th>
                  </tr>
                </ng-template>
                <ng-template pTemplate="body" let-review>
                  <tr class="hover:tw-bg-gray-50">
                    <td class="tw-px-3 tw-py-2">
                      <a [routerLink]="['/ethics/reviews', review.id, 'decision']"
                         class="tw-font-mono tw-text-blue-600 hover:tw-underline tw-text-xs">
                        {{ review.ethicsNumber }}
                      </a>
                    </td>
                    <td class="tw-px-3 tw-py-2 tw-text-xs tw-text-gray-600 tw-font-mono">
                      {{ review.studyId | slice:0:8 }}...
                    </td>
                    <td class="tw-px-3 tw-py-2 tw-text-xs">{{ review.reviewTypeLabel }}</td>
                    <td class="tw-px-3 tw-py-2 tw-text-xs">
                      {{ review.submissionDate | date:'dd/MM/yyyy' }}
                    </td>
                    <td class="tw-px-3 tw-py-2 tw-text-xs tw-text-gray-600">
                      {{ review.rapporteurName ?? '—' }}
                    </td>
                  </tr>
                </ng-template>
              </p-table>
            }
          </div>

          <!-- Prochaine réunion -->
          <div class="tw-bg-white tw-rounded-xl tw-border tw-border-gray-200 tw-shadow-sm">
            <div class="tw-flex tw-items-center tw-justify-between tw-px-5 tw-py-4 tw-border-b tw-border-gray-100">
              <h3 class="tw-font-semibold tw-text-gray-800">Prochaine réunion</h3>
              <a routerLink="/ethics/meetings"
                 class="tw-text-blue-600 tw-text-sm hover:tw-underline">Calendrier</a>
            </div>

            @if (dashboard()!.nextMeeting) {
              <div class="tw-p-5 tw-space-y-4">
                <div class="tw-flex tw-items-start tw-gap-3">
                  <div class="tw-w-10 tw-h-10 tw-bg-blue-100 tw-rounded-lg tw-flex tw-items-center tw-justify-center tw-shrink-0">
                    <i class="pi pi-calendar tw-text-blue-500"></i>
                  </div>
                  <div>
                    <p class="tw-font-semibold tw-text-gray-800">
                      {{ dashboard()!.nextMeeting!.meetingDate | date:'EEEE d MMMM yyyy':'':'fr' }}
                    </p>
                    @if (dashboard()!.nextMeeting!.meetingTime) {
                      <p class="tw-text-sm tw-text-gray-500">
                        à {{ dashboard()!.nextMeeting!.meetingTime }}
                      </p>
                    }
                  </div>
                </div>

                <div class="tw-flex tw-items-center tw-gap-2 tw-text-sm tw-text-gray-600">
                  <i class="pi pi-map-marker tw-text-gray-400"></i>
                  <span>{{ dashboard()!.nextMeeting!.location }}</span>
                </div>

                <div class="tw-flex tw-items-center tw-gap-2">
                  <p-tag
                    [value]="dashboard()!.nextMeeting!.meetingTypeLabel"
                    [severity]="getMeetingStatusSeverity(dashboard()!.nextMeeting!.status)"
                  />
                  <p-tag
                    [value]="dashboard()!.nextMeeting!.statusLabel"
                    severity="info"
                  />
                </div>

                <p-button
                  label="Voir la réunion"
                  icon="pi pi-arrow-right"
                  iconPos="right"
                  severity="secondary"
                  size="small"
                  [routerLink]="['/ethics/meetings', dashboard()!.nextMeeting!.id]"
                  styleClass="tw-w-full tw-mt-2"
                />
              </div>
            } @else {
              <div class="tw-py-10 tw-text-center tw-text-gray-400">
                <i class="pi pi-calendar tw-text-3xl tw-block tw-mb-2"></i>
                <p class="tw-text-sm">Aucune réunion planifiée</p>
                <p-button
                  label="Planifier une réunion"
                  icon="pi pi-plus"
                  severity="secondary"
                  size="small"
                  routerLink="/ethics/meetings"
                  styleClass="tw-mt-3"
                />
              </div>
            }
          </div>
        </div>
      }

    </div>
  `,
})
export class EthicsDashboardComponent implements OnInit {

  private readonly ethicsService = inject(EthicsService);
  private readonly router = inject(Router);

  /** Données du tableau de bord CE, null pendant le chargement. */
  protected readonly dashboard = signal<EthicsDashboardResponse | null>(null);
  /** Indicateur de chargement en cours. */
  protected readonly isLoading = signal(false);

  // ─────────────────────────────────────────────────────────────

  ngOnInit(): void {
    this.loadDashboard();
  }

  /**
   * Charge les données agrégées du tableau de bord depuis l'API.
   */
  protected loadDashboard(): void {
    this.isLoading.set(true);
    this.ethicsService.getDashboard().subscribe({
      next: (data) => {
        this.dashboard.set(data);
        this.isLoading.set(false);
      },
      error: (err) => {
        console.error('Erreur chargement dashboard CE', err);
        this.isLoading.set(false);
      },
    });
  }

  /**
   * Retourne la severité PrimeNG Tag pour un statut de réunion donné.
   *
   * @param status clé du statut de réunion
   * @returns severité PrimeNG (info, warning, success, danger)
   */
  protected getMeetingStatusSeverity(status: string): string {
    return MEETING_STATUS_SEVERITY[status] ?? 'info';
  }

  /**
   * Retourne la severité PrimeNG Tag pour une décision CE donnée.
   *
   * @param decision clé de la décision
   * @returns severité PrimeNG
   */
  protected getDecisionSeverity(decision: string): string {
    return DECISION_SEVERITY[decision] ?? 'info';
  }
}
