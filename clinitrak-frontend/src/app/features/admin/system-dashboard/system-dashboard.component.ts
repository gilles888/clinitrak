import { Component, computed, inject, OnInit, signal } from '@angular/core';
import { SlicePipe } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { KnobModule } from 'primeng/knob';
import { ProgressBarModule } from 'primeng/progressbar';
import { CardModule } from 'primeng/card';
import { TagModule } from 'primeng/tag';
import { ButtonModule } from 'primeng/button';
import { AdminService } from '../../../core/services/admin.service';
import { SystemHealth } from '../../../core/models/admin.model';

/**
 * Tableau de bord système de l'administration CliniTrak.
 *
 * <p>Affiche l'état de santé de chaque service via un knob global
 * et des cartes individuelles avec progress bar par service.
 */
@Component({
  selector: 'app-system-dashboard',
  standalone: true,
  imports: [SlicePipe, FormsModule, KnobModule, ProgressBarModule, CardModule, TagModule, ButtonModule],
  template: `
    <div class="tw-p-6">

      <!-- Titre -->
      <div class="tw-flex tw-justify-between tw-items-center tw-mb-6">
        <div>
          <h1 class="tw-text-2xl tw-font-bold tw-text-gray-900">Tableau de bord système</h1>
          <p class="tw-text-sm tw-text-gray-500 tw-mt-1">
            @if (health()?.checkedAt) {
              Dernière vérification : {{ health()!.checkedAt | slice:0:19 | slice:0:10 }}
              à {{ health()!.checkedAt | slice:11:19 }}
            } @else {
              Chargement...
            }
          </p>
        </div>
        <p-button
          label="Actualiser"
          icon="pi pi-refresh"
          severity="secondary"
          [loading]="isLoading()"
          (onClick)="loadHealth()"
        />
      </div>

      @if (isLoading() && !health()) {
        <div class="tw-text-center tw-py-16 tw-text-gray-400">
          <i class="pi pi-spin pi-spinner tw-text-4xl"></i>
          <p class="tw-mt-4">Vérification de l'état des services...</p>
        </div>
      } @else if (health()) {

        <!-- Score global -->
        <div class="tw-flex tw-flex-col tw-items-center tw-mb-10">
          <p class="tw-text-sm tw-font-medium tw-text-gray-600 tw-mb-4">État global du système</p>
          <p-knob
            [ngModel]="healthScore()"
            [readonly]="true"
            [size]="150"
            [valueColor]="health()!.status === 'UP' ? '#10b981' : '#ef4444'"
            rangeColor="#e5e7eb"
            textColor="#111827"
          />
          <div class="tw-mt-3">
            <p-tag
              [value]="health()!.status === 'UP' ? 'Tous les services opérationnels' : 'Dégradation détectée'"
              [severity]="health()!.status === 'UP' ? 'success' : 'danger'"
            />
          </div>
        </div>

        <!-- Grille des services -->
        <div class="tw-grid tw-grid-cols-1 md:tw-grid-cols-2 lg:tw-grid-cols-3 tw-gap-4">
          @for (entry of serviceEntries(); track entry.name) {
            <p-card>
              <div class="tw-flex tw-items-center tw-justify-between tw-mb-3">
                <h3 class="tw-font-semibold tw-text-gray-800 tw-capitalize">{{ entry.name }}</h3>
                <p-tag
                  [value]="entry.status"
                  [severity]="entry.status === 'UP' ? 'success' : 'danger'"
                />
              </div>
              <p-progressBar
                [value]="entry.status === 'UP' ? 100 : 0"
                [showValue]="false"
                styleClass="tw-h-2"
                [color]="entry.status === 'UP' ? '#10b981' : '#ef4444'"
              />
              <p class="tw-text-xs tw-text-gray-400 tw-mt-2">
                @if (entry.status === 'UP') {
                  Service opérationnel
                } @else {
                  Service indisponible
                }
              </p>
            </p-card>
          }
        </div>

      } @else {
        <div class="tw-text-center tw-py-16 tw-text-gray-400">
          <i class="pi pi-exclamation-triangle tw-text-4xl tw-block tw-mb-4"></i>
          <p>Impossible de récupérer l'état du système.</p>
          <p-button label="Réessayer" icon="pi pi-refresh" (onClick)="loadHealth()" styleClass="tw-mt-4" />
        </div>
      }

    </div>
  `,
})
export class SystemDashboardComponent implements OnInit {

  private readonly adminService = inject(AdminService);

  /** État de santé du système. */
  protected readonly health = signal<SystemHealth | null>(null);

  /** Indique si le chargement est en cours. */
  protected readonly isLoading = signal(false);

  /** Score global : 100 si UP, moyenne sinon. */
  protected readonly healthScore = computed(() => {
    const h = this.health();
    if (!h) return 0;
    if (h.status === 'UP') return 100;
    const services = Object.values(h.services);
    if (services.length === 0) return 0;
    const upCount = services.filter(s => s === 'UP').length;
    return Math.round((upCount / services.length) * 100);
  });

  /** Entrées des services formatées pour l'affichage. */
  protected readonly serviceEntries = computed(() => {
    const h = this.health();
    if (!h) return [];
    return Object.entries(h.services).map(([name, status]) => ({ name, status }));
  });

  /** Charge les données au démarrage. */
  ngOnInit(): void {
    this.loadHealth();
  }

  /**
   * Récupère l'état de santé depuis l'API.
   */
  protected loadHealth(): void {
    this.isLoading.set(true);
    this.adminService.getSystemHealth().subscribe({
      next: (h) => {
        this.health.set(h);
        this.isLoading.set(false);
      },
      error: () => this.isLoading.set(false),
    });
  }
}
