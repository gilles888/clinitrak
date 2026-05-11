import { Component, computed, inject, OnInit, signal } from '@angular/core';
import { CommonModule, DatePipe } from '@angular/common';
import { RouterLink } from '@angular/router';
import { BadgeModule } from 'primeng/badge';
import { CardModule } from 'primeng/card';
import { TagModule } from 'primeng/tag';
import { PharmacyService } from '../../../core/services/pharmacy.service';
import {
  AlertType,
  PharmacyAlert,
  PharmacyAlertsResponse,
} from '../../../core/models/pharmacy.model';

/**
 * Composant affichage des alertes pharmacie actives.
 *
 * <p>Charge les alertes via {@link PharmacyService#getAlerts} au init et les
 * regroupe par type à l'aide d'un computed signal.
 *
 * <p>Les sections sont affichées par ordre de criticité :
 * <ol>
 *   <li>Péremptions dans 7 jours (danger)</li>
 *   <li>Péremptions dans 30 jours (avertissement)</li>
 *   <li>Stocks faibles (avertissement)</li>
 *   <li>Articles en quarantaine (info)</li>
 * </ol>
 */
@Component({
  selector: 'app-expiry-alert',
  standalone: true,
  imports: [CommonModule, DatePipe, RouterLink, BadgeModule, CardModule, TagModule],
  template: `
    <div class="tw-space-y-4">

      <!-- En-tête -->
      <div class="tw-flex tw-items-center tw-justify-between">
        <div>
          <h2 class="tw-text-2xl tw-font-bold tw-text-gray-900">Alertes pharmacie</h2>
          <p class="tw-text-sm tw-text-gray-500 tw-mt-1">
            @if (alertsResponse()) {
              {{ alertsResponse()!.alerts.length }} alerte(s) active(s)
              @if (alertsResponse()!.criticalCount > 0) {
                — <span class="tw-text-red-600 tw-font-semibold">{{ alertsResponse()!.criticalCount }} critique(s)</span>
              }
            }
          </p>
        </div>
      </div>

      <!-- Chargement -->
      @if (isLoading()) {
        <div class="tw-flex tw-justify-center tw-py-12">
          <i class="pi pi-spin pi-spinner tw-text-4xl tw-text-blue-500"></i>
        </div>
      }

      <!-- Aucune alerte -->
      @if (!isLoading() && !alertsResponse()?.alerts?.length) {
        <div class="tw-p-12 tw-text-center tw-text-green-600">
          <i class="pi pi-check-circle tw-text-5xl tw-block tw-mb-3"></i>
          <p class="tw-text-lg tw-font-medium">Aucune alerte active</p>
          <p class="tw-text-sm tw-text-gray-500 tw-mt-1">Tous les stocks sont dans les limites normales.</p>
        </div>
      }

      <!-- Section péremptions J-7 (danger) -->
      @if (alertsByType()[AlertType.EXPIRY_7]?.length) {
        <p-card styleClass="tw-border tw-border-red-300 tw-bg-red-50">
          <ng-template pTemplate="header">
            <div class="tw-flex tw-items-center tw-gap-2 tw-px-4 tw-pt-4 tw-pb-2">
              <i class="pi pi-exclamation-triangle tw-text-red-500 tw-text-lg"></i>
              <span class="tw-font-bold tw-text-red-700">Péremptions dans 7 jours</span>
              <p-badge
                [value]="alertsByType()[AlertType.EXPIRY_7].length.toString()"
                severity="danger"
              />
            </div>
          </ng-template>
          <div class="tw-divide-y tw-divide-red-100">
            @for (alert of alertsByType()[AlertType.EXPIRY_7]; track alert.drugName) {
              <div class="tw-flex tw-justify-between tw-items-center tw-py-2 tw-px-1">
                <div>
                  <span class="tw-text-sm tw-font-medium tw-text-gray-800">{{ alert.drugName }}</span>
                  <span class="tw-text-xs tw-text-gray-500 tw-ml-2">— {{ alert.studyId | slice:0:8 }}…</span>
                </div>
                <span class="tw-text-sm tw-text-red-700 tw-font-medium">{{ alert.detail }}</span>
              </div>
            }
          </div>
        </p-card>
      }

      <!-- Section péremptions J-30 (avertissement) -->
      @if (alertsByType()[AlertType.EXPIRY_30]?.length) {
        <p-card styleClass="tw-border tw-border-amber-300 tw-bg-amber-50">
          <ng-template pTemplate="header">
            <div class="tw-flex tw-items-center tw-gap-2 tw-px-4 tw-pt-4 tw-pb-2">
              <i class="pi pi-clock tw-text-amber-500 tw-text-lg"></i>
              <span class="tw-font-bold tw-text-amber-700">Péremptions dans 30 jours</span>
              <p-badge
                [value]="alertsByType()[AlertType.EXPIRY_30].length.toString()"
                severity="warn"
              />
            </div>
          </ng-template>
          <div class="tw-divide-y tw-divide-amber-100">
            @for (alert of alertsByType()[AlertType.EXPIRY_30]; track alert.drugName) {
              <div class="tw-flex tw-justify-between tw-items-center tw-py-2 tw-px-1">
                <div>
                  <span class="tw-text-sm tw-font-medium tw-text-gray-800">{{ alert.drugName }}</span>
                  <span class="tw-text-xs tw-text-gray-500 tw-ml-2">— {{ alert.studyId | slice:0:8 }}…</span>
                </div>
                <span class="tw-text-sm tw-text-amber-700 tw-font-medium">{{ alert.detail }}</span>
              </div>
            }
          </div>
        </p-card>
      }

      <!-- Section stocks faibles (avertissement) -->
      @if (alertsByType()[AlertType.LOW_STOCK]?.length) {
        <p-card styleClass="tw-border tw-border-orange-300 tw-bg-orange-50">
          <ng-template pTemplate="header">
            <div class="tw-flex tw-items-center tw-gap-2 tw-px-4 tw-pt-4 tw-pb-2">
              <i class="pi pi-chart-bar tw-text-orange-500 tw-text-lg"></i>
              <span class="tw-font-bold tw-text-orange-700">Stocks faibles</span>
              <p-badge
                [value]="alertsByType()[AlertType.LOW_STOCK].length.toString()"
                severity="warn"
              />
            </div>
          </ng-template>
          <div class="tw-divide-y tw-divide-orange-100">
            @for (alert of alertsByType()[AlertType.LOW_STOCK]; track alert.drugName) {
              <div class="tw-flex tw-justify-between tw-items-center tw-py-2 tw-px-1">
                <div>
                  <span class="tw-text-sm tw-font-medium tw-text-gray-800">{{ alert.drugName }}</span>
                  <span class="tw-text-xs tw-text-gray-500 tw-ml-2">— {{ alert.studyId | slice:0:8 }}…</span>
                </div>
                <span class="tw-text-sm tw-text-orange-700 tw-font-medium">{{ alert.detail }}</span>
              </div>
            }
          </div>
        </p-card>
      }

      <!-- Section quarantaine (info) -->
      @if (alertsByType()[AlertType.QUARANTINE]?.length) {
        <p-card styleClass="tw-border tw-border-blue-300 tw-bg-blue-50">
          <ng-template pTemplate="header">
            <div class="tw-flex tw-items-center tw-gap-2 tw-px-4 tw-pt-4 tw-pb-2">
              <i class="pi pi-shield tw-text-blue-500 tw-text-lg"></i>
              <span class="tw-font-bold tw-text-blue-700">Articles en quarantaine</span>
              <p-badge
                [value]="alertsByType()[AlertType.QUARANTINE].length.toString()"
                severity="info"
              />
            </div>
          </ng-template>
          <div class="tw-divide-y tw-divide-blue-100">
            @for (alert of alertsByType()[AlertType.QUARANTINE]; track alert.drugName) {
              <div class="tw-flex tw-justify-between tw-items-center tw-py-2 tw-px-1">
                <div>
                  <span class="tw-text-sm tw-font-medium tw-text-gray-800">{{ alert.drugName }}</span>
                  <span class="tw-text-xs tw-text-gray-500 tw-ml-2">— {{ alert.studyId | slice:0:8 }}…</span>
                </div>
                <span class="tw-text-sm tw-text-blue-700 tw-font-medium">{{ alert.detail }}</span>
              </div>
            }
          </div>
        </p-card>
      }

    </div>
  `,
})
export class ExpiryAlertComponent implements OnInit {

  private readonly pharmacyService = inject(PharmacyService);

  /** Indicateur de chargement. */
  protected readonly isLoading = signal(false);

  /** Réponse brute de l'API alertes. */
  protected readonly alertsResponse = signal<PharmacyAlertsResponse | null>(null);

  /** Expose l'enum AlertType au template. */
  protected readonly AlertType = AlertType;

  /**
   * Alertes groupées par type (EXPIRY_7, EXPIRY_30, LOW_STOCK, QUARANTINE).
   */
  protected readonly alertsByType = computed<Record<AlertType, PharmacyAlert[]>>(() => {
    const a = this.alertsResponse();
    if (!a) {
      return {
        [AlertType.EXPIRY_7]:   [],
        [AlertType.EXPIRY_30]:  [],
        [AlertType.LOW_STOCK]:  [],
        [AlertType.QUARANTINE]: [],
      };
    }
    return {
      [AlertType.EXPIRY_7]:   a.alerts.filter(x => x.type === AlertType.EXPIRY_7),
      [AlertType.EXPIRY_30]:  a.alerts.filter(x => x.type === AlertType.EXPIRY_30),
      [AlertType.LOW_STOCK]:  a.alerts.filter(x => x.type === AlertType.LOW_STOCK),
      [AlertType.QUARANTINE]: a.alerts.filter(x => x.type === AlertType.QUARANTINE),
    };
  });

  // ─────────────────────────────────────────────────────────────

  ngOnInit(): void {
    this.loadAlerts();
  }

  /**
   * Charge les alertes pharmacie actives depuis l'API.
   */
  private loadAlerts(): void {
    this.isLoading.set(true);
    this.pharmacyService.getAlerts().subscribe({
      next: response => {
        this.alertsResponse.set(response);
        this.isLoading.set(false);
      },
      error: err => {
        console.error('Erreur chargement alertes', err);
        this.isLoading.set(false);
      },
    });
  }
}
