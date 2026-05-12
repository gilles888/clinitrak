import { Component, computed, inject, OnInit, signal } from '@angular/core';
import { CommonModule, DatePipe } from '@angular/common';
import { ReactiveFormsModule, FormBuilder, Validators } from '@angular/forms';
import { ButtonModule } from 'primeng/button';
import { CalendarModule } from 'primeng/calendar';
import { CardModule } from 'primeng/card';
import { ChartModule } from 'primeng/chart';
import { DialogModule } from 'primeng/dialog';
import { DropdownModule } from 'primeng/dropdown';
import { InputNumberModule } from 'primeng/inputnumber';
import { TableModule } from 'primeng/table';
import { TagModule } from 'primeng/tag';
import { CtcService } from '../../../core/services/ctc.service';
import {
  CONTRACT_STATUS_SEVERITY,
  CONTRACT_TYPE_OPTIONS,
  ContractStatus,
  ContractType,
  Currency,
  FinancialContract,
} from '../../../core/models/ctc.model';

/** Options devise. */
const CURRENCY_OPTIONS = [
  { label: 'EUR (€)', value: Currency.EUR },
  { label: 'USD ($)', value: Currency.USD },
  { label: 'GBP (£)', value: Currency.GBP },
];

/**
 * Composant tableau de bord financier CTC.
 *
 * <p>Présente trois KPI cards (nombre total de contrats, montant total en EUR,
 * contrats actifs), deux charts PrimeNG (doughnut répartition par statut, bar
 * montants par type) et un tableau complet des contrats.
 *
 * <p>Les données des charts sont calculées via des {@link computed} réactifs
 * depuis la liste des contrats.
 */
@Component({
  selector: 'app-financial-dashboard',
  standalone: true,
  imports: [
    CommonModule,
    DatePipe,
    ReactiveFormsModule,
    ButtonModule,
    CalendarModule,
    CardModule,
    ChartModule,
    DialogModule,
    DropdownModule,
    InputNumberModule,
    TableModule,
    TagModule,
  ],
  template: `
    <div class="tw-space-y-6">

      <!-- En-tête -->
      <div class="tw-flex tw-items-center tw-justify-between">
        <div>
          <h2 class="tw-text-2xl tw-font-bold tw-text-gray-900">Contrats financiers</h2>
          <p class="tw-text-sm tw-text-gray-500 tw-mt-1">Gestion des conventions et contrats d'études</p>
        </div>
        <p-button
          label="Nouveau contrat"
          icon="pi pi-plus"
          severity="primary"
          (onClick)="openCreateDialog()"
        />
      </div>

      <!-- Chargement -->
      @if (isLoading()) {
        <div class="tw-flex tw-justify-center tw-py-12">
          <i class="pi pi-spin pi-spinner tw-text-4xl tw-text-blue-500"></i>
        </div>
      }

      @if (!isLoading()) {

        <!-- KPI Cards -->
        <div class="tw-grid tw-grid-cols-1 sm:tw-grid-cols-3 tw-gap-4">

          <div class="tw-bg-white tw-rounded-xl tw-border tw-border-gray-200 tw-p-5 tw-flex tw-items-center tw-gap-4 tw-shadow-sm">
            <div class="tw-w-12 tw-h-12 tw-bg-blue-100 tw-rounded-full tw-flex tw-items-center tw-justify-center tw-shrink-0">
              <i class="pi pi-file tw-text-blue-500 tw-text-xl"></i>
            </div>
            <div>
              <p class="tw-text-3xl tw-font-bold tw-text-blue-600">{{ contracts().length }}</p>
              <p class="tw-text-sm tw-text-gray-600 tw-mt-0.5">Total contrats</p>
            </div>
          </div>

          <div class="tw-bg-white tw-rounded-xl tw-border tw-border-gray-200 tw-p-5 tw-flex tw-items-center tw-gap-4 tw-shadow-sm">
            <div class="tw-w-12 tw-h-12 tw-bg-green-100 tw-rounded-full tw-flex tw-items-center tw-justify-center tw-shrink-0">
              <i class="pi pi-euro tw-text-green-500 tw-text-xl"></i>
            </div>
            <div>
              <p class="tw-text-2xl tw-font-bold tw-text-green-600">
                {{ totalAmount() | number:'1.0-0' }} €
              </p>
              <p class="tw-text-sm tw-text-gray-600 tw-mt-0.5">Montant total (EUR)</p>
            </div>
          </div>

          <div class="tw-bg-white tw-rounded-xl tw-border tw-border-gray-200 tw-p-5 tw-flex tw-items-center tw-gap-4 tw-shadow-sm">
            <div class="tw-w-12 tw-h-12 tw-bg-emerald-100 tw-rounded-full tw-flex tw-items-center tw-justify-center tw-shrink-0">
              <i class="pi pi-check-circle tw-text-emerald-500 tw-text-xl"></i>
            </div>
            <div>
              <p class="tw-text-3xl tw-font-bold tw-text-emerald-600">{{ activeContractsCount() }}</p>
              <p class="tw-text-sm tw-text-gray-600 tw-mt-0.5">Contrats actifs</p>
            </div>
          </div>

        </div>

        <!-- Charts -->
        <div class="tw-grid tw-grid-cols-1 lg:tw-grid-cols-2 tw-gap-6">

          <!-- Doughnut : répartition par statut -->
          <div class="tw-bg-white tw-rounded-xl tw-border tw-border-gray-200 tw-p-5 tw-shadow-sm">
            <h3 class="tw-font-semibold tw-text-gray-800 tw-mb-4">Répartition par statut</h3>
            @if (contracts().length > 0) {
              <p-chart
                type="doughnut"
                [data]="doughnutData()"
                [options]="doughnutOptions"
                height="250px"
              />
            } @else {
              <div class="tw-py-8 tw-text-center tw-text-gray-400">
                <p class="tw-text-sm">Aucune donnée disponible</p>
              </div>
            }
          </div>

          <!-- Bar : montants par type -->
          <div class="tw-bg-white tw-rounded-xl tw-border tw-border-gray-200 tw-p-5 tw-shadow-sm">
            <h3 class="tw-font-semibold tw-text-gray-800 tw-mb-4">Montants par type (EUR)</h3>
            @if (contracts().length > 0) {
              <p-chart
                type="bar"
                [data]="barData()"
                [options]="barOptions"
                height="250px"
              />
            } @else {
              <div class="tw-py-8 tw-text-center tw-text-gray-400">
                <p class="tw-text-sm">Aucune donnée disponible</p>
              </div>
            }
          </div>

        </div>

        <!-- Tableau des contrats -->
        <div class="tw-bg-white tw-rounded-xl tw-border tw-border-gray-200 tw-shadow-sm tw-overflow-hidden">
          <p-table
            [value]="contracts()"
            [scrollable]="true"
            [paginator]="true"
            [rows]="10"
            emptyMessage="Aucun contrat financier trouvé"
            styleClass="tw-text-sm"
          >
            <ng-template pTemplate="header">
              <tr class="tw-bg-gray-50">
                <th class="tw-font-semibold tw-text-xs tw-text-gray-600 tw-px-3 tw-py-3">Étude</th>
                <th class="tw-font-semibold tw-text-xs tw-text-gray-600 tw-px-3 tw-py-3">Type</th>
                <th class="tw-font-semibold tw-text-xs tw-text-gray-600 tw-px-3 tw-py-3">Date</th>
                <th class="tw-font-semibold tw-text-xs tw-text-gray-600 tw-px-3 tw-py-3">Montant</th>
                <th class="tw-font-semibold tw-text-xs tw-text-gray-600 tw-px-3 tw-py-3">Devise</th>
                <th class="tw-font-semibold tw-text-xs tw-text-gray-600 tw-px-3 tw-py-3">Statut</th>
              </tr>
            </ng-template>
            <ng-template pTemplate="body" let-contract>
              <tr class="hover:tw-bg-gray-50">
                <td class="tw-px-3 tw-py-3 tw-font-mono tw-text-xs tw-text-gray-600">
                  {{ contract.studyId | slice:0:8 }}...
                </td>
                <td class="tw-px-3 tw-py-3 tw-text-xs">{{ contract.contractTypeLabel }}</td>
                <td class="tw-px-3 tw-py-3 tw-text-xs">{{ contract.contractDate | date:'dd/MM/yyyy' }}</td>
                <td class="tw-px-3 tw-py-3 tw-text-xs tw-font-semibold tw-text-gray-800">
                  {{ contract.amount | number:'1.2-2' }}
                </td>
                <td class="tw-px-3 tw-py-3 tw-text-xs tw-text-gray-500">{{ contract.currencyLabel }}</td>
                <td class="tw-px-3 tw-py-3">
                  <p-tag
                    [value]="contract.statusLabel"
                    [severity]="getContractStatusSeverity(contract)"
                  />
                </td>
              </tr>
            </ng-template>
          </p-table>
        </div>

      }

      <!-- Dialog : Nouveau contrat -->
      <p-dialog
        header="Nouveau contrat financier"
        [(visible)]="showCreateDialog"
        [modal]="true"
        [style]="{ width: '520px' }"
        [draggable]="false"
      >
        <form [formGroup]="createForm" class="tw-space-y-4 tw-pt-2">

          <div class="tw-flex tw-flex-col tw-gap-1">
            <label class="tw-text-sm tw-font-medium tw-text-gray-700">
              ID Étude <span class="tw-text-red-500">*</span>
            </label>
            <input pInputText formControlName="studyId" placeholder="UUID de l'étude" class="tw-w-full tw-font-mono" />
            @if (createForm.get('studyId')?.invalid && createForm.get('studyId')?.touched) {
              <span class="tw-text-xs tw-text-red-500">Ce champ est obligatoire.</span>
            }
          </div>

          <div class="tw-flex tw-flex-col tw-gap-1">
            <label class="tw-text-sm tw-font-medium tw-text-gray-700">
              Type de contrat <span class="tw-text-red-500">*</span>
            </label>
            <p-dropdown
              formControlName="contractType"
              [options]="contractTypeOptions"
              optionLabel="label"
              optionValue="value"
              placeholder="Sélectionner le type"
              styleClass="tw-w-full"
            />
          </div>

          <div class="tw-flex tw-flex-col tw-gap-1">
            <label class="tw-text-sm tw-font-medium tw-text-gray-700">
              Date du contrat <span class="tw-text-red-500">*</span>
            </label>
            <p-calendar
              formControlName="contractDate"
              dateFormat="dd/mm/yy"
              [showIcon]="true"
              styleClass="tw-w-full"
            />
          </div>

          <div class="tw-grid tw-grid-cols-2 tw-gap-3">
            <div class="tw-flex tw-flex-col tw-gap-1">
              <label class="tw-text-sm tw-font-medium tw-text-gray-700">
                Montant <span class="tw-text-red-500">*</span>
              </label>
              <p-inputNumber
                formControlName="amount"
                [minFractionDigits]="2"
                [maxFractionDigits]="2"
                placeholder="0.00"
                styleClass="tw-w-full"
              />
            </div>
            <div class="tw-flex tw-flex-col tw-gap-1">
              <label class="tw-text-sm tw-font-medium tw-text-gray-700">
                Devise <span class="tw-text-red-500">*</span>
              </label>
              <p-dropdown
                formControlName="currency"
                [options]="currencyOptions"
                optionLabel="label"
                optionValue="value"
                placeholder="Devise"
                styleClass="tw-w-full"
              />
            </div>
          </div>

          <div class="tw-flex tw-flex-col tw-gap-1">
            <label class="tw-text-sm tw-font-medium tw-text-gray-700">Conditions de paiement</label>
            <input pInputText formControlName="paymentTerms" placeholder="Ex : 30 jours fin de mois" class="tw-w-full" />
          </div>

        </form>

        <ng-template pTemplate="footer">
          <div class="tw-flex tw-justify-end tw-gap-2">
            <p-button label="Annuler" severity="secondary" (onClick)="closeCreateDialog()" />
            <p-button
              label="Créer le contrat"
              icon="pi pi-check"
              [loading]="isSubmitting()"
              [disabled]="createForm.invalid"
              (onClick)="submitCreate()"
            />
          </div>
        </ng-template>
      </p-dialog>

    </div>
  `,
})
export class FinancialDashboardComponent implements OnInit {

  private readonly ctcService = inject(CtcService);
  private readonly fb = inject(FormBuilder);

  /** Liste des contrats financiers. */
  protected readonly contracts = signal<FinancialContract[]>([]);

  /** Indicateur de chargement. */
  protected readonly isLoading = signal(false);

  /** Indicateur de soumission. */
  protected readonly isSubmitting = signal(false);

  /** Visibilité du dialog de création. */
  protected showCreateDialog = false;

  // Options de dropdowns
  protected readonly contractTypeOptions = CONTRACT_TYPE_OPTIONS;
  protected readonly currencyOptions = CURRENCY_OPTIONS;

  /** Montant total EUR (approximatif, sans conversion devises). */
  protected readonly totalAmount = computed(() =>
    this.contracts()
      .filter(c => c.currency === Currency.EUR)
      .reduce((sum, c) => sum + c.amount, 0),
  );

  /** Nombre de contrats actifs. */
  protected readonly activeContractsCount = computed(() =>
    this.contracts().filter(c => c.status === ContractStatus.ACTIVE).length,
  );

  /** Données pour le doughnut chart (répartition par statut). */
  protected readonly doughnutData = computed(() => {
    const statusCounts: Record<string, number> = {};
    for (const c of this.contracts()) {
      statusCounts[c.statusLabel] = (statusCounts[c.statusLabel] ?? 0) + 1;
    }
    return {
      labels: Object.keys(statusCounts),
      datasets: [{
        data: Object.values(statusCounts),
        backgroundColor: ['#10b981', '#3b82f6', '#6366f1', '#ef4444'],
      }],
    };
  });

  /** Données pour le bar chart (montants par type de contrat). */
  protected readonly barData = computed(() => {
    const typeTotals: Record<string, number> = {};
    for (const c of this.contracts()) {
      if (c.currency === Currency.EUR) {
        typeTotals[c.contractTypeLabel] = (typeTotals[c.contractTypeLabel] ?? 0) + c.amount;
      }
    }
    return {
      labels: Object.keys(typeTotals),
      datasets: [{
        label: 'Montant (EUR)',
        data: Object.values(typeTotals),
        backgroundColor: '#3b82f6',
      }],
    };
  });

  /** Options du doughnut chart. */
  protected readonly doughnutOptions = {
    responsive: true,
    plugins: { legend: { position: 'bottom' } },
  };

  /** Options du bar chart. */
  protected readonly barOptions = {
    responsive: true,
    plugins: { legend: { display: false } },
    scales: { y: { beginAtZero: true } },
  };

  /** Formulaire de création de contrat. */
  protected createForm = this.fb.group({
    studyId:      ['', Validators.required],
    contractType: [null, Validators.required],
    contractDate: [null, Validators.required],
    amount:       [null, Validators.required],
    currency:     [Currency.EUR, Validators.required],
    paymentTerms: [''],
  });

  // ─────────────────────────────────────────────────────────────

  ngOnInit(): void {
    this.loadContracts();
  }

  /**
   * Charge la liste des contrats financiers depuis l'API.
   */
  protected loadContracts(): void {
    this.isLoading.set(true);
    this.ctcService.getFinancialContracts().subscribe({
      next: (data) => {
        this.contracts.set(data);
        this.isLoading.set(false);
      },
      error: (err) => {
        console.error('Erreur chargement contrats financiers', err);
        this.isLoading.set(false);
      },
    });
  }

  /**
   * Ouvre le dialog de création de contrat.
   */
  protected openCreateDialog(): void {
    this.createForm.reset({ currency: Currency.EUR });
    this.showCreateDialog = true;
  }

  /**
   * Ferme le dialog de création.
   */
  protected closeCreateDialog(): void {
    this.showCreateDialog = false;
  }

  /**
   * Soumet la création d'un nouveau contrat financier.
   */
  protected submitCreate(): void {
    if (this.createForm.invalid) {
      this.createForm.markAllAsTouched();
      return;
    }
    const raw = this.createForm.value;
    const payload: Partial<FinancialContract> = {
      studyId:      raw.studyId!,
      contractType: (raw.contractType ?? '') as ContractType,
      contractDate: this.formatDate(raw.contractDate as unknown as Date),
      amount:       raw.amount!,
      currency:     raw.currency as Currency,
      paymentTerms: raw.paymentTerms || undefined,
    };
    this.isSubmitting.set(true);
    this.ctcService.createFinancialContract(payload).subscribe({
      next: (created) => {
        this.contracts.update(list => [created, ...list]);
        this.showCreateDialog = false;
        this.isSubmitting.set(false);
      },
      error: (err) => {
        console.error('Erreur création contrat financier', err);
        this.isSubmitting.set(false);
      },
    });
  }

  /**
   * Retourne la sévérité PrimeNG Tag pour le statut d'un contrat.
   *
   * @param contract contrat financier
   * @returns sévérité PrimeNG
   */
  protected getContractStatusSeverity(contract: FinancialContract): 'success' | 'info' | 'secondary' | 'contrast' | 'warning' | 'danger' | undefined {
    return CONTRACT_STATUS_SEVERITY[contract.status] ?? 'info';
  }

  /**
   * Formate un objet Date en chaîne ISO 8601.
   *
   * @param date valeur issue du calendrier PrimeNG
   * @returns chaîne yyyy-MM-dd
   */
  private formatDate(date: Date): string {
    const y = date.getFullYear();
    const m = String(date.getMonth() + 1).padStart(2, '0');
    const d = String(date.getDate()).padStart(2, '0');
    return `${y}-${m}-${d}`;
  }
}
