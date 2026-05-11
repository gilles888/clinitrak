import { Component, inject, OnInit, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterLink } from '@angular/router';
import { ButtonModule } from 'primeng/button';
import { CardModule } from 'primeng/card';
import { TagModule } from 'primeng/tag';
import { PharmacyService } from '../../../core/services/pharmacy.service';
import { PharmacyDashboard } from '../../../core/models/pharmacy.model';

/**
 * Composant tableau de bord du module Pharmacie.
 *
 * <p>Affiche les 6 KPIs principaux (médicaments, stocks disponibles, stocks faibles,
 * péremptions J-30, dispensations, levées d'aveugle ouvertes) ainsi que les alertes
 * urgentes et des liens rapides vers les sous-modules.
 *
 * <p>Les données sont chargées via {@link PharmacyService#getDashboard} au init.
 */
@Component({
  selector: 'app-pharmacy-dashboard',
  standalone: true,
  imports: [CommonModule, RouterLink, ButtonModule, CardModule, TagModule],
  template: `
    <div class="tw-space-y-6">

      <!-- En-tête -->
      <div>
        <h2 class="tw-text-2xl tw-font-bold tw-text-gray-900">Pharmacie</h2>
        <p class="tw-text-gray-500 tw-mt-1">Gestion des médicaments expérimentaux</p>
      </div>

      <!-- KPIs -->
      <div class="tw-grid tw-grid-cols-2 lg:tw-grid-cols-3 tw-gap-4">
        @if (isLoading()) {
          @for (i of [1, 2, 3, 4, 5, 6]; track i) {
            <p-card styleClass="tw-border tw-border-gray-100">
              <div class="tw-h-16 tw-bg-gray-100 tw-animate-pulse tw-rounded"></div>
            </p-card>
          }
        } @else if (dashboard()) {
          <p-card styleClass="tw-border tw-border-gray-100">
            <p class="tw-text-sm tw-text-gray-500">Médicaments</p>
            <p class="tw-text-3xl tw-font-bold tw-text-blue-600">{{ dashboard()!.totalDrugs }}</p>
          </p-card>

          <p-card styleClass="tw-border tw-border-gray-100">
            <p class="tw-text-sm tw-text-gray-500">Stocks disponibles</p>
            <p class="tw-text-3xl tw-font-bold tw-text-green-600">{{ dashboard()!.availableStocks }}</p>
          </p-card>

          <p-card
            [styleClass]="dashboard()!.lowStockCount > 0
              ? 'tw-border tw-border-red-300 tw-bg-red-50'
              : 'tw-border tw-border-gray-100'"
          >
            <p class="tw-text-sm tw-text-gray-500">Stocks faibles</p>
            <p
              [class]="dashboard()!.lowStockCount > 0
                ? 'tw-text-3xl tw-font-bold tw-text-red-600'
                : 'tw-text-3xl tw-font-bold tw-text-gray-900'"
            >{{ dashboard()!.lowStockCount }}</p>
          </p-card>

          <p-card styleClass="tw-border tw-border-gray-100">
            <p class="tw-text-sm tw-text-gray-500">Péremptions J-30</p>
            <p class="tw-text-3xl tw-font-bold tw-text-amber-600">{{ dashboard()!.expiringIn30Days }}</p>
          </p-card>

          <p-card styleClass="tw-border tw-border-gray-100">
            <p class="tw-text-sm tw-text-gray-500">Dispenses</p>
            <p class="tw-text-3xl tw-font-bold tw-text-gray-900">{{ dashboard()!.pendingDispensations }}</p>
          </p-card>

          <p-card
            [styleClass]="dashboard()!.openUnblindings > 0
              ? 'tw-border tw-border-orange-300'
              : 'tw-border tw-border-gray-100'"
          >
            <p class="tw-text-sm tw-text-gray-500">Levées aveugle</p>
            <p class="tw-text-3xl tw-font-bold tw-text-orange-600">{{ dashboard()!.openUnblindings }}</p>
          </p-card>
        }
      </div>

      <!-- Alertes urgentes -->
      @if (dashboard()?.urgentAlerts?.length) {
        <p-card header="Alertes urgentes" styleClass="tw-border tw-border-red-200 tw-bg-red-50">
          <div class="tw-space-y-2">
            @for (alert of dashboard()!.urgentAlerts; track alert.drugName) {
              <div class="tw-flex tw-items-center tw-gap-3 tw-p-2 tw-bg-white tw-rounded tw-border tw-border-red-100">
                <i class="pi pi-exclamation-triangle tw-text-red-500"></i>
                <div>
                  <p class="tw-text-sm tw-font-medium">{{ alert.drugName }} — {{ alert.typeLabel }}</p>
                  <p class="tw-text-xs tw-text-gray-500">{{ alert.detail }}</p>
                </div>
              </div>
            }
          </div>
        </p-card>
      }

      <!-- Liens rapides -->
      <div class="tw-grid tw-grid-cols-2 lg:tw-grid-cols-4 tw-gap-4">
        <a
          routerLink="/pharmacy/stocks"
          class="tw-block tw-p-4 tw-bg-white tw-border tw-border-gray-200 tw-rounded-lg hover:tw-shadow-md tw-transition-shadow tw-text-center"
        >
          <i class="pi pi-box tw-text-2xl tw-text-blue-500"></i>
          <p class="tw-mt-2 tw-text-sm tw-font-medium">Stocks</p>
        </a>
        <a
          routerLink="/pharmacy/dispensations"
          class="tw-block tw-p-4 tw-bg-white tw-border tw-border-gray-200 tw-rounded-lg hover:tw-shadow-md tw-transition-shadow tw-text-center"
        >
          <i class="pi pi-send tw-text-2xl tw-text-green-500"></i>
          <p class="tw-mt-2 tw-text-sm tw-font-medium">Dispensations</p>
        </a>
        <a
          routerLink="/pharmacy/alerts"
          class="tw-block tw-p-4 tw-bg-white tw-border tw-border-gray-200 tw-rounded-lg hover:tw-shadow-md tw-transition-shadow tw-text-center"
        >
          <i class="pi pi-bell tw-text-2xl tw-text-amber-500"></i>
          <p class="tw-mt-2 tw-text-sm tw-font-medium">Alertes</p>
        </a>
        <a
          routerLink="/pharmacy/emergency"
          class="tw-block tw-p-4 tw-bg-white tw-border tw-border-red-200 tw-rounded-lg hover:tw-shadow-md tw-transition-shadow tw-text-center"
        >
          <i class="pi pi-lock-open tw-text-2xl tw-text-red-500"></i>
          <p class="tw-mt-2 tw-text-sm tw-font-medium">Levée aveugle</p>
        </a>
      </div>

      <!-- Liens secondaires -->
      <div class="tw-grid tw-grid-cols-2 lg:tw-grid-cols-3 tw-gap-4">
        <a
          routerLink="/pharmacy/receive"
          class="tw-block tw-p-4 tw-bg-white tw-border tw-border-gray-200 tw-rounded-lg hover:tw-shadow-md tw-transition-shadow tw-text-center"
        >
          <i class="pi pi-download tw-text-2xl tw-text-indigo-500"></i>
          <p class="tw-mt-2 tw-text-sm tw-font-medium">Réception stock</p>
        </a>
        <a
          routerLink="/pharmacy/reports"
          class="tw-block tw-p-4 tw-bg-white tw-border tw-border-gray-200 tw-rounded-lg hover:tw-shadow-md tw-transition-shadow tw-text-center"
        >
          <i class="pi pi-file-pdf tw-text-2xl tw-text-red-500"></i>
          <p class="tw-mt-2 tw-text-sm tw-font-medium">Rapports</p>
        </a>
      </div>

    </div>
  `,
})
export class PharmacyDashboardComponent implements OnInit {

  private readonly pharmacyService = inject(PharmacyService);

  /** Indicateur de chargement en cours. */
  protected readonly isLoading = signal(false);

  /** Données agrégées du dashboard pharmacie, null pendant le chargement. */
  protected readonly dashboard = signal<PharmacyDashboard | null>(null);

  // ─────────────────────────────────────────────────────────────

  ngOnInit(): void {
    this.loadDashboard();
  }

  /**
   * Charge les données agrégées du tableau de bord depuis l'API.
   */
  private loadDashboard(): void {
    this.isLoading.set(true);
    this.pharmacyService.getDashboard().subscribe({
      next: d => {
        this.dashboard.set(d);
        this.isLoading.set(false);
      },
      error: err => {
        console.error('Erreur chargement dashboard pharmacie', err);
        this.isLoading.set(false);
      },
    });
  }
}
