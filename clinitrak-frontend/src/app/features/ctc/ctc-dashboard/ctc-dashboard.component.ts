import { Component, inject, OnInit, signal } from '@angular/core';
import { CommonModule, DatePipe } from '@angular/common';
import { RouterLink } from '@angular/router';
import { ButtonModule } from 'primeng/button';
import { CardModule } from 'primeng/card';
import { TableModule } from 'primeng/table';
import { TagModule } from 'primeng/tag';
import { CtcService } from '../../../core/services/ctc.service';
import {
  CTCDashboard,
  PRIORITY_SEVERITY,
  REQUEST_STATUS_SEVERITY,
  TrialDeskRequest,
} from '../../../core/models/ctc.model';

/**
 * Composant tableau de bord du module CTC (Clinical Trial Center).
 *
 * <p>Affiche les 5 KPI cards principaux (demandes en attente, visites planifiées,
 * événements qualité ouverts, événements critiques, contrats actifs) ainsi qu'un
 * tableau des dernières demandes guichet avec liens vers chaque sous-module.
 *
 * <p>Les données sont récupérées via {@link CtcService#getDashboard} au moment
 * de l'initialisation du composant.
 */
@Component({
  selector: 'app-ctc-dashboard',
  standalone: true,
  imports: [
    CommonModule,
    DatePipe,
    RouterLink,
    ButtonModule,
    CardModule,
    TableModule,
    TagModule,
  ],
  template: `
    <div class="tw-space-y-6">

      <!-- En-tête -->
      <div class="tw-flex tw-items-center tw-justify-between">
        <div>
          <h2 class="tw-text-2xl tw-font-bold tw-text-gray-900">Centre de Thérapie Clinique (CTC)</h2>
          <p class="tw-text-sm tw-text-gray-500 tw-mt-1">Vue d'ensemble et indicateurs clés</p>
        </div>
        <p-button
          label="Nouvelle demande guichet"
          icon="pi pi-plus"
          routerLink="/ctc/desk-requests"
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

        <!-- KPI Cards — 5 indicateurs -->
        <div class="tw-grid tw-grid-cols-1 sm:tw-grid-cols-2 xl:tw-grid-cols-5 tw-gap-4">

          <!-- Demandes en attente -->
          <div class="tw-bg-white tw-rounded-xl tw-border tw-border-gray-200 tw-p-5 tw-flex tw-items-center tw-gap-4 tw-shadow-sm">
            <div class="tw-w-12 tw-h-12 tw-bg-orange-100 tw-rounded-full tw-flex tw-items-center tw-justify-center tw-shrink-0">
              <i class="pi pi-inbox tw-text-orange-500 tw-text-xl"></i>
            </div>
            <div>
              <p class="tw-text-3xl tw-font-bold tw-text-orange-600">{{ dashboard()!.pendingRequests }}</p>
              <p class="tw-text-sm tw-text-gray-600 tw-mt-0.5">Demandes en attente</p>
            </div>
          </div>

          <!-- Visites planifiées -->
          <div class="tw-bg-white tw-rounded-xl tw-border tw-border-gray-200 tw-p-5 tw-flex tw-items-center tw-gap-4 tw-shadow-sm">
            <div class="tw-w-12 tw-h-12 tw-bg-blue-100 tw-rounded-full tw-flex tw-items-center tw-justify-center tw-shrink-0">
              <i class="pi pi-calendar tw-text-blue-500 tw-text-xl"></i>
            </div>
            <div>
              <p class="tw-text-3xl tw-font-bold tw-text-blue-600">{{ dashboard()!.plannedVisits }}</p>
              <p class="tw-text-sm tw-text-gray-600 tw-mt-0.5">Visites planifiées</p>
            </div>
          </div>

          <!-- Événements qualité ouverts -->
          <div class="tw-bg-white tw-rounded-xl tw-border tw-border-gray-200 tw-p-5 tw-flex tw-items-center tw-gap-4 tw-shadow-sm">
            <div class="tw-w-12 tw-h-12 tw-bg-yellow-100 tw-rounded-full tw-flex tw-items-center tw-justify-center tw-shrink-0">
              <i class="pi pi-exclamation-circle tw-text-yellow-500 tw-text-xl"></i>
            </div>
            <div>
              <p class="tw-text-3xl tw-font-bold tw-text-yellow-600">{{ dashboard()!.openQualityEvents }}</p>
              <p class="tw-text-sm tw-text-gray-600 tw-mt-0.5">Événements qualité ouverts</p>
            </div>
          </div>

          <!-- Événements critiques -->
          <div class="tw-bg-white tw-rounded-xl tw-border tw-border-gray-200 tw-p-5 tw-flex tw-items-center tw-gap-4 tw-shadow-sm">
            <div class="tw-w-12 tw-h-12 tw-rounded-full tw-flex tw-items-center tw-justify-center tw-shrink-0"
                 [class]="dashboard()!.criticalEvents > 0 ? 'tw-bg-red-100' : 'tw-bg-gray-100'">
              <i class="pi pi-exclamation-triangle tw-text-xl"
                 [class]="dashboard()!.criticalEvents > 0 ? 'tw-text-red-500' : 'tw-text-gray-400'"></i>
            </div>
            <div>
              <p class="tw-text-3xl tw-font-bold"
                 [class]="dashboard()!.criticalEvents > 0 ? 'tw-text-red-600' : 'tw-text-gray-700'">
                {{ dashboard()!.criticalEvents }}
              </p>
              <p class="tw-text-sm tw-text-gray-600 tw-mt-0.5">Événements critiques</p>
            </div>
          </div>

          <!-- Contrats actifs -->
          <div class="tw-bg-white tw-rounded-xl tw-border tw-border-gray-200 tw-p-5 tw-flex tw-items-center tw-gap-4 tw-shadow-sm">
            <div class="tw-w-12 tw-h-12 tw-bg-green-100 tw-rounded-full tw-flex tw-items-center tw-justify-center tw-shrink-0">
              <i class="pi pi-file-edit tw-text-green-500 tw-text-xl"></i>
            </div>
            <div>
              <p class="tw-text-3xl tw-font-bold tw-text-green-600">{{ dashboard()!.activeContracts }}</p>
              <p class="tw-text-sm tw-text-gray-600 tw-mt-0.5">Contrats actifs</p>
            </div>
          </div>

        </div>

        <!-- Alerte événements critiques -->
        @if (dashboard()!.criticalEvents > 0) {
          <div class="tw-bg-red-50 tw-border tw-border-red-200 tw-rounded-lg tw-p-4 tw-flex tw-items-center tw-justify-between">
            <div class="tw-flex tw-items-center tw-gap-3">
              <i class="pi pi-exclamation-triangle tw-text-red-500 tw-text-xl"></i>
              <span class="tw-text-red-700 tw-font-medium">
                {{ dashboard()!.criticalEvents }} événement(s) qualité critique(s) en cours
              </span>
            </div>
            <a routerLink="/ctc/quality"
               class="tw-text-red-600 tw-underline tw-text-sm tw-font-semibold hover:tw-text-red-800">
              Voir les événements
            </a>
          </div>
        }

        <!-- Liens vers les sous-modules -->
        <div class="tw-grid tw-grid-cols-2 md:tw-grid-cols-3 lg:tw-grid-cols-6 tw-gap-3">
          <a routerLink="/ctc/desk-requests"
             class="tw-flex tw-flex-col tw-items-center tw-gap-2 tw-bg-white tw-border tw-border-gray-200
                    tw-rounded-xl tw-p-4 tw-shadow-sm hover:tw-border-blue-400 hover:tw-shadow-md tw-transition-all">
            <i class="pi pi-inbox tw-text-2xl tw-text-blue-500"></i>
            <span class="tw-text-xs tw-font-medium tw-text-gray-700 tw-text-center">Guichet</span>
          </a>
          <a routerLink="/ctc/monitoring"
             class="tw-flex tw-flex-col tw-items-center tw-gap-2 tw-bg-white tw-border tw-border-gray-200
                    tw-rounded-xl tw-p-4 tw-shadow-sm hover:tw-border-blue-400 hover:tw-shadow-md tw-transition-all">
            <i class="pi pi-map tw-text-2xl tw-text-indigo-500"></i>
            <span class="tw-text-xs tw-font-medium tw-text-gray-700 tw-text-center">Monitoring</span>
          </a>
          <a routerLink="/ctc/financials"
             class="tw-flex tw-flex-col tw-items-center tw-gap-2 tw-bg-white tw-border tw-border-gray-200
                    tw-rounded-xl tw-p-4 tw-shadow-sm hover:tw-border-blue-400 hover:tw-shadow-md tw-transition-all">
            <i class="pi pi-euro tw-text-2xl tw-text-green-500"></i>
            <span class="tw-text-xs tw-font-medium tw-text-gray-700 tw-text-center">Financier</span>
          </a>
          <a routerLink="/ctc/quality"
             class="tw-flex tw-flex-col tw-items-center tw-gap-2 tw-bg-white tw-border tw-border-gray-200
                    tw-rounded-xl tw-p-4 tw-shadow-sm hover:tw-border-blue-400 hover:tw-shadow-md tw-transition-all">
            <i class="pi pi-shield tw-text-2xl tw-text-red-500"></i>
            <span class="tw-text-xs tw-font-medium tw-text-gray-700 tw-text-center">Qualité</span>
          </a>
          <a routerLink="/ctc/sponsor-studies"
             class="tw-flex tw-flex-col tw-items-center tw-gap-2 tw-bg-white tw-border tw-border-gray-200
                    tw-rounded-xl tw-p-4 tw-shadow-sm hover:tw-border-blue-400 hover:tw-shadow-md tw-transition-all">
            <i class="pi pi-building tw-text-2xl tw-text-purple-500"></i>
            <span class="tw-text-xs tw-font-medium tw-text-gray-700 tw-text-center">Promoteur</span>
          </a>
          <a routerLink="/ctc/statistics"
             class="tw-flex tw-flex-col tw-items-center tw-gap-2 tw-bg-white tw-border tw-border-gray-200
                    tw-rounded-xl tw-p-4 tw-shadow-sm hover:tw-border-blue-400 hover:tw-shadow-md tw-transition-all">
            <i class="pi pi-chart-bar tw-text-2xl tw-text-teal-500"></i>
            <span class="tw-text-xs tw-font-medium tw-text-gray-700 tw-text-center">Statistiques</span>
          </a>
        </div>

        <!-- Tableau des dernières demandes guichet -->
        <div class="tw-bg-white tw-rounded-xl tw-border tw-border-gray-200 tw-shadow-sm">
          <div class="tw-flex tw-items-center tw-justify-between tw-px-5 tw-py-4 tw-border-b tw-border-gray-100">
            <h3 class="tw-font-semibold tw-text-gray-800">Dernières demandes guichet</h3>
            <a routerLink="/ctc/desk-requests"
               class="tw-text-blue-600 tw-text-sm hover:tw-underline">Voir tout</a>
          </div>

          @if (dashboard()!.recentRequests.length === 0) {
            <div class="tw-py-10 tw-text-center tw-text-gray-400">
              <i class="pi pi-inbox tw-text-3xl tw-block tw-mb-2"></i>
              <p class="tw-text-sm">Aucune demande récente</p>
            </div>
          } @else {
            <p-table [value]="dashboard()!.recentRequests" styleClass="tw-text-sm">
              <ng-template pTemplate="header">
                <tr class="tw-bg-gray-50">
                  <th class="tw-font-semibold tw-text-xs tw-text-gray-600 tw-px-3 tw-py-2">Étude</th>
                  <th class="tw-font-semibold tw-text-xs tw-text-gray-600 tw-px-3 tw-py-2">Demandeur</th>
                  <th class="tw-font-semibold tw-text-xs tw-text-gray-600 tw-px-3 tw-py-2">Type</th>
                  <th class="tw-font-semibold tw-text-xs tw-text-gray-600 tw-px-3 tw-py-2">Priorité</th>
                  <th class="tw-font-semibold tw-text-xs tw-text-gray-600 tw-px-3 tw-py-2">Statut</th>
                  <th class="tw-font-semibold tw-text-xs tw-text-gray-600 tw-px-3 tw-py-2">Date</th>
                </tr>
              </ng-template>
              <ng-template pTemplate="body" let-req>
                <tr class="hover:tw-bg-gray-50">
                  <td class="tw-px-3 tw-py-2 tw-font-mono tw-text-xs tw-text-gray-600">
                    {{ req.studyId | slice:0:8 }}...
                  </td>
                  <td class="tw-px-3 tw-py-2 tw-text-xs">{{ req.requestorName }}</td>
                  <td class="tw-px-3 tw-py-2 tw-text-xs tw-text-gray-600">{{ req.requestTypeLabel }}</td>
                  <td class="tw-px-3 tw-py-2">
                    <p-tag
                      [value]="req.priorityLabel"
                      [severity]="getPrioritySeverity(req)"
                    />
                  </td>
                  <td class="tw-px-3 tw-py-2">
                    <p-tag
                      [value]="req.statusLabel"
                      [severity]="getStatusSeverity(req)"
                    />
                  </td>
                  <td class="tw-px-3 tw-py-2 tw-text-xs tw-text-gray-500">
                    {{ req.requestDate | date:'dd/MM/yyyy' }}
                  </td>
                </tr>
              </ng-template>
            </p-table>
          }
        </div>

      }

    </div>
  `,
})
export class CTCDashboardComponent implements OnInit {

  private readonly ctcService = inject(CtcService);

  /** Données agrégées du dashboard CTC, null pendant le chargement. */
  protected readonly dashboard = signal<CTCDashboard | null>(null);

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
    this.ctcService.getDashboard().subscribe({
      next: (data) => {
        this.dashboard.set(data);
        this.isLoading.set(false);
      },
      error: (err) => {
        console.error('Erreur chargement dashboard CTC', err);
        this.isLoading.set(false);
      },
    });
  }

  /**
   * Retourne la sévérité PrimeNG Tag pour le statut d'une demande.
   *
   * @param req demande guichet
   * @returns sévérité PrimeNG
   */
  protected getStatusSeverity(req: TrialDeskRequest): 'success' | 'info' | 'secondary' | 'contrast' | 'warning' | 'danger' | undefined {
    return REQUEST_STATUS_SEVERITY[req.status] ?? 'info';
  }

  /**
   * Retourne la sévérité PrimeNG Tag pour la priorité d'une demande.
   *
   * @param req demande guichet
   * @returns sévérité PrimeNG
   */
  protected getPrioritySeverity(req: TrialDeskRequest): 'success' | 'info' | 'secondary' | 'contrast' | 'warning' | 'danger' | undefined {
    return PRIORITY_SEVERITY[req.priority] ?? 'info';
  }
}
