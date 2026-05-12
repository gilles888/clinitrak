import { Component, inject, OnInit, signal } from '@angular/core';
import { CommonModule, DatePipe } from '@angular/common';
import { ReactiveFormsModule, FormBuilder, Validators } from '@angular/forms';
import { ButtonModule } from 'primeng/button';
import { CalendarModule } from 'primeng/calendar';
import { DialogModule } from 'primeng/dialog';
import { DropdownModule } from 'primeng/dropdown';
import { InputTextModule } from 'primeng/inputtext';
import { TableModule } from 'primeng/table';
import { TagModule } from 'primeng/tag';
import { CtcService } from '../../../core/services/ctc.service';
import {
  ANALYSIS_TYPE_OPTIONS,
  StatisticsRequest,
  STATISTICS_STATUS_SEVERITY,
} from '../../../core/models/ctc.model';

/** Options de format de données statistiques. */
const DATA_FORMAT_OPTIONS = [
  { label: 'SAS (.sas7bdat)',   value: 'SAS' },
  { label: 'SPSS (.sav)',       value: 'SPSS' },
  { label: 'R (.rds)',          value: 'R' },
  { label: 'CSV',               value: 'CSV' },
  { label: 'Excel (.xlsx)',     value: 'EXCEL' },
];

/**
 * Composant demandes d'analyse statistique CTC.
 *
 * <p>Affiche un tableau des demandes existantes et propose un dialog de création.
 * Le statut est représenté par un {@code p-tag} PrimeNG avec des sévérités
 * issues de {@link STATISTICS_STATUS_SEVERITY}.
 */
@Component({
  selector: 'app-statistics-request-form',
  standalone: true,
  imports: [
    CommonModule,
    DatePipe,
    ReactiveFormsModule,
    ButtonModule,
    CalendarModule,
    DialogModule,
    DropdownModule,
    InputTextModule,
    TableModule,
    TagModule,
  ],
  template: `
    <div class="tw-space-y-6">

      <!-- En-tête -->
      <div class="tw-flex tw-items-center tw-justify-between">
        <div>
          <h2 class="tw-text-2xl tw-font-bold tw-text-gray-900">Demandes statistiques</h2>
          <p class="tw-text-sm tw-text-gray-500 tw-mt-1">
            {{ requests().length }} demande(s) au total
          </p>
        </div>
        <p-button
          label="Nouvelle demande"
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

      <!-- Tableau -->
      @if (!isLoading()) {
        <div class="tw-bg-white tw-rounded-xl tw-border tw-border-gray-200 tw-shadow-sm tw-overflow-hidden">
          <p-table
            [value]="requests()"
            [scrollable]="true"
            [paginator]="true"
            [rows]="15"
            emptyMessage="Aucune demande statistique trouvée"
            styleClass="tw-text-sm"
          >
            <ng-template pTemplate="header">
              <tr class="tw-bg-gray-50">
                <th class="tw-font-semibold tw-text-xs tw-text-gray-600 tw-px-3 tw-py-3">Étude</th>
                <th class="tw-font-semibold tw-text-xs tw-text-gray-600 tw-px-3 tw-py-3">Demandeur</th>
                <th class="tw-font-semibold tw-text-xs tw-text-gray-600 tw-px-3 tw-py-3">Deadline</th>
                <th class="tw-font-semibold tw-text-xs tw-text-gray-600 tw-px-3 tw-py-3">Type d'analyse</th>
                <th class="tw-font-semibold tw-text-xs tw-text-gray-600 tw-px-3 tw-py-3">Format</th>
                <th class="tw-font-semibold tw-text-xs tw-text-gray-600 tw-px-3 tw-py-3">Statut</th>
                <th class="tw-font-semibold tw-text-xs tw-text-gray-600 tw-px-3 tw-py-3">Date livraison</th>
              </tr>
            </ng-template>
            <ng-template pTemplate="body" let-req>
              <tr class="hover:tw-bg-gray-50">
                <td class="tw-px-3 tw-py-3 tw-font-mono tw-text-xs tw-text-gray-600">
                  {{ req.studyId | slice:0:8 }}...
                </td>
                <td class="tw-px-3 tw-py-3 tw-text-xs">{{ req.requestorName }}</td>
                <td class="tw-px-3 tw-py-3 tw-text-xs"
                    [class]="isDeadlineSoon(req.deadline) ? 'tw-text-orange-600 tw-font-semibold' : 'tw-text-gray-600'">
                  {{ req.deadline | date:'dd/MM/yyyy' }}
                </td>
                <td class="tw-px-3 tw-py-3 tw-text-xs">{{ req.analysisTypeLabel }}</td>
                <td class="tw-px-3 tw-py-3 tw-text-xs tw-text-gray-500">{{ req.dataFormatLabel }}</td>
                <td class="tw-px-3 tw-py-3">
                  <p-tag
                    [value]="req.statusLabel"
                    [severity]="getStatisticsStatusSeverity(req)"
                  />
                </td>
                <td class="tw-px-3 tw-py-3 tw-text-xs tw-text-gray-500">
                  {{ req.deliveredDate ? (req.deliveredDate | date:'dd/MM/yyyy') : '—' }}
                </td>
              </tr>
            </ng-template>
          </p-table>
        </div>
      }

      <!-- Dialog : Nouvelle demande statistique -->
      <p-dialog
        header="Nouvelle demande statistique"
        [(visible)]="showCreateDialog"
        [modal]="true"
        [style]="{ width: '500px' }"
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
              Demandeur <span class="tw-text-red-500">*</span>
            </label>
            <input pInputText formControlName="requestorName" placeholder="Nom du demandeur" class="tw-w-full" />
            @if (createForm.get('requestorName')?.invalid && createForm.get('requestorName')?.touched) {
              <span class="tw-text-xs tw-text-red-500">Ce champ est obligatoire.</span>
            }
          </div>

          <div class="tw-flex tw-flex-col tw-gap-1">
            <label class="tw-text-sm tw-font-medium tw-text-gray-700">
              Deadline <span class="tw-text-red-500">*</span>
            </label>
            <p-calendar
              formControlName="deadline"
              dateFormat="dd/mm/yy"
              [showIcon]="true"
              styleClass="tw-w-full"
            />
            @if (createForm.get('deadline')?.invalid && createForm.get('deadline')?.touched) {
              <span class="tw-text-xs tw-text-red-500">Ce champ est obligatoire.</span>
            }
          </div>

          <div class="tw-flex tw-flex-col tw-gap-1">
            <label class="tw-text-sm tw-font-medium tw-text-gray-700">
              Type d'analyse <span class="tw-text-red-500">*</span>
            </label>
            <p-dropdown
              formControlName="analysisType"
              [options]="analysisTypeOptions"
              optionLabel="label"
              optionValue="value"
              placeholder="Sélectionner le type"
              styleClass="tw-w-full"
            />
          </div>

          <div class="tw-flex tw-flex-col tw-gap-1">
            <label class="tw-text-sm tw-font-medium tw-text-gray-700">
              Format des données <span class="tw-text-red-500">*</span>
            </label>
            <p-dropdown
              formControlName="dataFormat"
              [options]="dataFormatOptions"
              optionLabel="label"
              optionValue="value"
              placeholder="Sélectionner le format"
              styleClass="tw-w-full"
            />
          </div>

        </form>

        <ng-template pTemplate="footer">
          <div class="tw-flex tw-justify-end tw-gap-2">
            <p-button label="Annuler" severity="secondary" (onClick)="closeCreateDialog()" />
            <p-button
              label="Soumettre la demande"
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
export class StatisticsRequestFormComponent implements OnInit {

  private readonly ctcService = inject(CtcService);
  private readonly fb = inject(FormBuilder);

  /** Liste des demandes statistiques. */
  protected readonly requests = signal<StatisticsRequest[]>([]);

  /** Indicateur de chargement. */
  protected readonly isLoading = signal(false);

  /** Indicateur de soumission. */
  protected readonly isSubmitting = signal(false);

  /** Visibilité du dialog de création. */
  protected showCreateDialog = false;

  // Options de dropdowns
  protected readonly analysisTypeOptions = ANALYSIS_TYPE_OPTIONS;
  protected readonly dataFormatOptions = DATA_FORMAT_OPTIONS;

  /** Formulaire de création d'une demande statistique. */
  protected createForm = this.fb.group({
    studyId:       ['', Validators.required],
    requestorName: ['', Validators.required],
    deadline:      [null, Validators.required],
    analysisType:  [null, Validators.required],
    dataFormat:    [null, Validators.required],
  });

  // ─────────────────────────────────────────────────────────────

  ngOnInit(): void {
    this.loadRequests();
  }

  /**
   * Charge la liste des demandes statistiques depuis l'API.
   */
  protected loadRequests(): void {
    this.isLoading.set(true);
    this.ctcService.getStatisticsRequests().subscribe({
      next: (data) => {
        this.requests.set(data);
        this.isLoading.set(false);
      },
      error: (err) => {
        console.error('Erreur chargement demandes statistiques', err);
        this.isLoading.set(false);
      },
    });
  }

  /**
   * Ouvre le dialog de création de demande statistique.
   */
  protected openCreateDialog(): void {
    this.createForm.reset();
    this.showCreateDialog = true;
  }

  /**
   * Ferme le dialog de création.
   */
  protected closeCreateDialog(): void {
    this.showCreateDialog = false;
  }

  /**
   * Soumet la création d'une nouvelle demande statistique.
   */
  protected submitCreate(): void {
    if (this.createForm.invalid) {
      this.createForm.markAllAsTouched();
      return;
    }
    const raw = this.createForm.value;
    const payload: Partial<StatisticsRequest> = {
      studyId:       raw.studyId!,
      requestorName: raw.requestorName!,
      deadline:      this.formatDate(raw.deadline as unknown as Date),
      analysisType:  raw.analysisType!,
      dataFormat:    raw.dataFormat!,
    };
    this.isSubmitting.set(true);
    this.ctcService.createStatisticsRequest(payload).subscribe({
      next: (created) => {
        this.requests.update(list => [created, ...list]);
        this.showCreateDialog = false;
        this.isSubmitting.set(false);
      },
      error: (err) => {
        console.error('Erreur création demande statistique', err);
        this.isSubmitting.set(false);
      },
    });
  }

  /**
   * Retourne la sévérité PrimeNG Tag pour le statut d'une demande statistique.
   *
   * @param req demande statistique
   * @returns sévérité PrimeNG
   */
  protected getStatisticsStatusSeverity(req: StatisticsRequest): 'success' | 'info' | 'secondary' | 'contrast' | 'warning' | 'danger' | undefined {
    return STATISTICS_STATUS_SEVERITY[req.status] ?? 'info';
  }

  /**
   * Indique si la deadline d'une demande approche (dans moins de 7 jours).
   *
   * @param deadline date ISO 8601
   * @returns vrai si la deadline est dans moins de 7 jours
   */
  protected isDeadlineSoon(deadline: string): boolean {
    const diff = new Date(deadline).getTime() - Date.now();
    return diff > 0 && diff < 7 * 24 * 60 * 60 * 1000;
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
