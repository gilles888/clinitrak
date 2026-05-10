import { Component, inject, OnInit, signal } from '@angular/core';
import { CommonModule, DatePipe } from '@angular/common';
import { RouterLink } from '@angular/router';
import { ButtonModule } from 'primeng/button';
import { TableModule } from 'primeng/table';
import { TagModule } from 'primeng/tag';
import { CtcService } from '../../../core/services/ctc.service';
import {
  REGULATORY_STATUS_SEVERITY,
  SponsorStudy,
} from '../../../core/models/ctc.model';

/**
 * Composant tableau de bord des études dont le CUSL est promoteur.
 *
 * <p>Affiche pour chaque étude : l'ID étude, le statut réglementaire,
 * une barre de progression budget (budgetSpent / budgetTotal),
 * le PM et le nombre de CRA associés.
 *
 * <p>Le lien vers le détail d'une étude est disponible via le composant
 * {@link CTCStudyDetailComponent} (route {@code /ctc/study/:studyId}).
 */
@Component({
  selector: 'app-sponsor-study-dashboard',
  standalone: true,
  imports: [
    CommonModule,
    DatePipe,
    RouterLink,
    ButtonModule,
    TableModule,
    TagModule,
  ],
  template: `
    <div class="tw-space-y-6">

      <!-- En-tête -->
      <div class="tw-flex tw-items-center tw-justify-between">
        <div>
          <h2 class="tw-text-2xl tw-font-bold tw-text-gray-900">Études promoteur</h2>
          <p class="tw-text-sm tw-text-gray-500 tw-mt-1">Études dont le CUSL est promoteur</p>
        </div>
        <p-button
          label="Voir le tableau de bord CTC"
          icon="pi pi-arrow-left"
          severity="secondary"
          [outlined]="true"
          routerLink="/ctc"
        />
      </div>

      <!-- Chargement -->
      @if (isLoading()) {
        <div class="tw-flex tw-justify-center tw-py-12">
          <i class="pi pi-spin pi-spinner tw-text-4xl tw-text-blue-500"></i>
        </div>
      }

      <!-- Tableau -->
      @if (!isLoading()) {
        <div class="tw-bg-white tw-rounded-xl tw-border tw-border-gray-200 tw-shadow-sm tw-overflow-hidden">
          <p-table
            [value]="studies()"
            [scrollable]="true"
            [paginator]="true"
            [rows]="15"
            emptyMessage="Aucune étude promoteur trouvée"
            styleClass="tw-text-sm"
          >
            <ng-template pTemplate="header">
              <tr class="tw-bg-gray-50">
                <th class="tw-font-semibold tw-text-xs tw-text-gray-600 tw-px-3 tw-py-3">ID Étude</th>
                <th class="tw-font-semibold tw-text-xs tw-text-gray-600 tw-px-3 tw-py-3">Statut réglementaire</th>
                <th class="tw-font-semibold tw-text-xs tw-text-gray-600 tw-px-3 tw-py-3 tw-w-48">Budget</th>
                <th class="tw-font-semibold tw-text-xs tw-text-gray-600 tw-px-3 tw-py-3">PM</th>
                <th class="tw-font-semibold tw-text-xs tw-text-gray-600 tw-px-3 tw-py-3">Nb CRA</th>
                <th class="tw-font-semibold tw-text-xs tw-text-gray-600 tw-px-3 tw-py-3">Actions</th>
              </tr>
            </ng-template>
            <ng-template pTemplate="body" let-study>
              <tr class="hover:tw-bg-gray-50">

                <!-- ID Étude -->
                <td class="tw-px-3 tw-py-4 tw-font-mono tw-text-xs tw-text-blue-600">
                  <a [routerLink]="['/ctc/study', study.studyId]"
                     class="hover:tw-underline">
                    {{ study.studyId | slice:0:8 }}...
                  </a>
                </td>

                <!-- Statut réglementaire -->
                <td class="tw-px-3 tw-py-4">
                  <p-tag
                    [value]="study.regulatoryStatusLabel"
                    [severity]="getRegulatoryStatusSeverity(study)"
                  />
                </td>

                <!-- Barre de progression budget -->
                <td class="tw-px-3 tw-py-4">
                  @if (study.budgetTotal && study.budgetSpent !== undefined) {
                    <div class="tw-space-y-1">
                      <div class="tw-flex tw-justify-between tw-text-xs tw-text-gray-500">
                        <span>{{ study.budgetSpent | number:'1.0-0' }} €</span>
                        <span>{{ study.budgetTotal | number:'1.0-0' }} €</span>
                      </div>
                      <div class="tw-w-full tw-bg-gray-200 tw-rounded-full tw-h-2">
                        <div
                          class="tw-h-2 tw-rounded-full tw-transition-all"
                          [class]="getBudgetBarClass(study)"
                          [style.width]="getBudgetPercent(study) + '%'"
                        ></div>
                      </div>
                      <p class="tw-text-xs tw-text-gray-400 tw-text-right">
                        {{ getBudgetPercent(study) | number:'1.0-1' }}%
                      </p>
                    </div>
                  } @else {
                    <span class="tw-text-xs tw-text-gray-300">Non défini</span>
                  }
                </td>

                <!-- Project Manager -->
                <td class="tw-px-3 tw-py-4 tw-text-xs tw-text-gray-700">
                  {{ study.projectManagerId ?? '—' }}
                </td>

                <!-- Nombre de CRA -->
                <td class="tw-px-3 tw-py-4">
                  <span class="tw-bg-blue-100 tw-text-blue-700 tw-text-xs tw-font-semibold
                               tw-px-2 tw-py-0.5 tw-rounded-full">
                    {{ study.craIds.length }}
                  </span>
                </td>

                <!-- Actions -->
                <td class="tw-px-3 tw-py-4">
                  <p-button
                    icon="pi pi-eye"
                    severity="secondary"
                    [outlined]="true"
                    size="small"
                    [routerLink]="['/ctc/study', study.studyId]"
                    pTooltip="Voir le détail"
                  />
                </td>

              </tr>
            </ng-template>
          </p-table>
        </div>
      }

    </div>
  `,
})
export class SponsorStudyDashboardComponent implements OnInit {

  private readonly ctcService = inject(CtcService);

  /** Liste des études promoteur. */
  protected readonly studies = signal<SponsorStudy[]>([]);

  /** Indicateur de chargement. */
  protected readonly isLoading = signal(false);

  // ─────────────────────────────────────────────────────────────

  ngOnInit(): void {
    this.loadStudies();
  }

  /**
   * Charge la liste des études promoteur depuis l'API.
   */
  protected loadStudies(): void {
    this.isLoading.set(true);
    this.ctcService.getSponsorStudies().subscribe({
      next: (data) => {
        this.studies.set(data);
        this.isLoading.set(false);
      },
      error: (err) => {
        console.error('Erreur chargement études promoteur', err);
        this.isLoading.set(false);
      },
    });
  }

  /**
   * Retourne le pourcentage du budget consommé (0–100, plafonné à 100).
   *
   * @param study étude promoteur
   * @returns pourcentage consommé
   */
  protected getBudgetPercent(study: SponsorStudy): number {
    if (!study.budgetTotal || !study.budgetSpent) return 0;
    return Math.min((study.budgetSpent / study.budgetTotal) * 100, 100);
  }

  /**
   * Retourne la classe CSS de la barre de budget selon le niveau de consommation.
   *
   * @param study étude promoteur
   * @returns classe Tailwind
   */
  protected getBudgetBarClass(study: SponsorStudy): string {
    const pct = this.getBudgetPercent(study);
    if (pct >= 90) return 'tw-bg-red-500';
    if (pct >= 70) return 'tw-bg-yellow-500';
    return 'tw-bg-green-500';
  }

  /**
   * Retourne la sévérité PrimeNG Tag pour le statut réglementaire d'une étude.
   *
   * @param study étude promoteur
   * @returns sévérité PrimeNG
   */
  protected getRegulatoryStatusSeverity(study: SponsorStudy): string {
    return REGULATORY_STATUS_SEVERITY[study.regulatoryStatus] ?? 'info';
  }
}
