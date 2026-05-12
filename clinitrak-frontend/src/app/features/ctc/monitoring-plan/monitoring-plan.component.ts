import { Component, computed, inject, OnInit, signal } from '@angular/core';
import { CommonModule, DatePipe } from '@angular/common';
import { ReactiveFormsModule, FormsModule, FormBuilder, Validators } from '@angular/forms';
import { ButtonModule } from 'primeng/button';
import { CalendarModule } from 'primeng/calendar';
import { DialogModule } from 'primeng/dialog';
import { DropdownModule } from 'primeng/dropdown';
import { InputTextModule } from 'primeng/inputtext';
import { TableModule } from 'primeng/table';
import { TagModule } from 'primeng/tag';
import { CtcService } from '../../../core/services/ctc.service';
import {
  MonitoringVisit,
  VISIT_STATUS_SEVERITY,
  VISIT_TYPE_OPTIONS,
  VisitStatus,
  VisitType,
} from '../../../core/models/ctc.model';

/** Options de statut pour le filtre. */
const VISIT_STATUS_OPTIONS = [
  { label: 'Planifiée',  value: VisitStatus.PLANNED },
  { label: 'Effectuée', value: VisitStatus.COMPLETED },
  { label: 'Annulée',   value: VisitStatus.CANCELLED },
];

/**
 * Composant plan de monitoring CTC.
 *
 * <p>Affiche un tableau de toutes les visites de monitoring avec possibilité
 * de filtrer par type et statut via des signals Angular et un computed.
 * Un dialog permet de planifier une nouvelle visite.
 */
@Component({
  selector: 'app-monitoring-plan',
  standalone: true,
  imports: [
    CommonModule,
    DatePipe,
    ReactiveFormsModule,
    FormsModule,
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
          <h2 class="tw-text-2xl tw-font-bold tw-text-gray-900">Plan de monitoring</h2>
          <p class="tw-text-sm tw-text-gray-500 tw-mt-1">
            {{ filteredVisits().length }} visite(s) affichée(s)
          </p>
        </div>
        <p-button
          label="Planifier une visite"
          icon="pi pi-plus"
          severity="primary"
          (onClick)="openCreateDialog()"
        />
      </div>

      <!-- Filtres -->
      <div class="tw-flex tw-flex-wrap tw-gap-3 tw-bg-white tw-rounded-xl tw-border tw-border-gray-200 tw-p-4 tw-shadow-sm">
        <div class="tw-flex tw-flex-col tw-gap-1 tw-min-w-[180px]">
          <label class="tw-text-xs tw-font-medium tw-text-gray-600">Type de visite</label>
          <p-dropdown
            [options]="visitTypeOptionsWithAll"
            [(ngModel)]="selectedType"
            optionLabel="label"
            optionValue="value"
            placeholder="Tous les types"
            [showClear]="true"
            styleClass="tw-w-full"
            (onChange)="filterType.set($event.value)"
          />
        </div>
        <div class="tw-flex tw-flex-col tw-gap-1 tw-min-w-[180px]">
          <label class="tw-text-xs tw-font-medium tw-text-gray-600">Statut</label>
          <p-dropdown
            [options]="visitStatusOptionsWithAll"
            [(ngModel)]="selectedStatus"
            optionLabel="label"
            optionValue="value"
            placeholder="Tous les statuts"
            [showClear]="true"
            styleClass="tw-w-full"
            (onChange)="filterStatus.set($event.value)"
          />
        </div>
        <div class="tw-flex tw-items-end">
          <p-button
            label="Réinitialiser"
            icon="pi pi-filter-slash"
            severity="secondary"
            [outlined]="true"
            size="small"
            (onClick)="resetFilters()"
          />
        </div>
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
            [value]="filteredVisits()"
            [scrollable]="true"
            [paginator]="true"
            [rows]="15"
            emptyMessage="Aucune visite de monitoring trouvée"
            styleClass="tw-text-sm"
          >
            <ng-template pTemplate="header">
              <tr class="tw-bg-gray-50">
                <th class="tw-font-semibold tw-text-xs tw-text-gray-600 tw-px-3 tw-py-3">Étude</th>
                <th class="tw-font-semibold tw-text-xs tw-text-gray-600 tw-px-3 tw-py-3">Date</th>
                <th class="tw-font-semibold tw-text-xs tw-text-gray-600 tw-px-3 tw-py-3">Type</th>
                <th class="tw-font-semibold tw-text-xs tw-text-gray-600 tw-px-3 tw-py-3">CRA</th>
                <th class="tw-font-semibold tw-text-xs tw-text-gray-600 tw-px-3 tw-py-3">Statut</th>
                <th class="tw-font-semibold tw-text-xs tw-text-gray-600 tw-px-3 tw-py-3">Deadline correction</th>
                <th class="tw-font-semibold tw-text-xs tw-text-gray-600 tw-px-3 tw-py-3">Findings</th>
              </tr>
            </ng-template>
            <ng-template pTemplate="body" let-visit>
              <tr class="hover:tw-bg-gray-50">
                <td class="tw-px-3 tw-py-3 tw-font-mono tw-text-xs tw-text-gray-600">
                  {{ visit.studyId | slice:0:8 }}...
                </td>
                <td class="tw-px-3 tw-py-3 tw-text-xs">{{ visit.visitDate | date:'dd/MM/yyyy' }}</td>
                <td class="tw-px-3 tw-py-3 tw-text-xs">{{ visit.visitTypeLabel }}</td>
                <td class="tw-px-3 tw-py-3 tw-text-xs tw-text-gray-700">{{ visit.monitorName }}</td>
                <td class="tw-px-3 tw-py-3">
                  <p-tag
                    [value]="visit.statusLabel"
                    [severity]="getVisitStatusSeverity(visit)"
                  />
                </td>
                <td class="tw-px-3 tw-py-3 tw-text-xs tw-text-gray-500">
                  @if (visit.correctionDeadline) {
                    <span [class]="isDeadlineExpired(visit.correctionDeadline) ? 'tw-text-red-600 tw-font-semibold' : ''">
                      {{ visit.correctionDeadline | date:'dd/MM/yyyy' }}
                    </span>
                  } @else {
                    <span class="tw-text-gray-300">—</span>
                  }
                </td>
                <td class="tw-px-3 tw-py-3 tw-text-xs tw-text-gray-500 tw-max-w-[200px] tw-truncate">
                  {{ visit.findings ?? '—' }}
                </td>
              </tr>
            </ng-template>
          </p-table>
        </div>
      }

      <!-- Dialog : Planifier une visite -->
      <p-dialog
        header="Planifier une visite de monitoring"
        [(visible)]="showCreateDialog"
        [modal]="true"
        [style]="{ width: '480px' }"
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
              Date de visite <span class="tw-text-red-500">*</span>
            </label>
            <p-calendar
              formControlName="visitDate"
              dateFormat="dd/mm/yy"
              [showIcon]="true"
              styleClass="tw-w-full"
            />
            @if (createForm.get('visitDate')?.invalid && createForm.get('visitDate')?.touched) {
              <span class="tw-text-xs tw-text-red-500">Ce champ est obligatoire.</span>
            }
          </div>

          <div class="tw-flex tw-flex-col tw-gap-1">
            <label class="tw-text-sm tw-font-medium tw-text-gray-700">
              Type de visite <span class="tw-text-red-500">*</span>
            </label>
            <p-dropdown
              formControlName="visitType"
              [options]="visitTypeOptions"
              optionLabel="label"
              optionValue="value"
              placeholder="Sélectionner le type"
              styleClass="tw-w-full"
            />
          </div>

          <div class="tw-flex tw-flex-col tw-gap-1">
            <label class="tw-text-sm tw-font-medium tw-text-gray-700">
              Nom du CRA <span class="tw-text-red-500">*</span>
            </label>
            <input pInputText formControlName="monitorName" placeholder="Nom du moniteur" class="tw-w-full" />
            @if (createForm.get('monitorName')?.invalid && createForm.get('monitorName')?.touched) {
              <span class="tw-text-xs tw-text-red-500">Ce champ est obligatoire.</span>
            }
          </div>

        </form>

        <ng-template pTemplate="footer">
          <div class="tw-flex tw-justify-end tw-gap-2">
            <p-button label="Annuler" severity="secondary" (onClick)="closeCreateDialog()" />
            <p-button
              label="Planifier"
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
export class MonitoringPlanComponent implements OnInit {

  private readonly ctcService = inject(CtcService);
  private readonly fb = inject(FormBuilder);

  /** Liste complète des visites de monitoring. */
  private readonly visits = signal<MonitoringVisit[]>([]);

  /** Indicateur de chargement. */
  protected readonly isLoading = signal(false);

  /** Indicateur de soumission. */
  protected readonly isSubmitting = signal(false);

  /** Filtre actif sur le type de visite. */
  protected readonly filterType = signal<VisitType | null>(null);

  /** Filtre actif sur le statut de visite. */
  protected readonly filterStatus = signal<VisitStatus | null>(null);

  /** Valeur liée au dropdown type (ngModel). */
  protected selectedType: VisitType | null = null;

  /** Valeur liée au dropdown statut (ngModel). */
  protected selectedStatus: VisitStatus | null = null;

  /** Visibilité du dialog de création. */
  protected showCreateDialog = false;

  // Options
  protected readonly visitTypeOptions = VISIT_TYPE_OPTIONS;
  protected readonly visitTypeOptionsWithAll = [{ label: 'Tous les types', value: null }, ...VISIT_TYPE_OPTIONS];
  protected readonly visitStatusOptionsWithAll = [{ label: 'Tous les statuts', value: null }, ...VISIT_STATUS_OPTIONS];

  /** Visites filtrées selon les signaux actifs. */
  protected readonly filteredVisits = computed(() => {
    let result = this.visits();
    const type = this.filterType();
    const status = this.filterStatus();
    if (type)   result = result.filter(v => v.visitType === type);
    if (status) result = result.filter(v => v.status === status);
    return result;
  });

  /** Formulaire de création d'une visite. */
  protected createForm = this.fb.group({
    studyId:     ['', Validators.required],
    visitDate:   [null, Validators.required],
    visitType:   [null, Validators.required],
    monitorName: ['', Validators.required],
  });

  // ─────────────────────────────────────────────────────────────

  ngOnInit(): void {
    this.loadVisits();
  }

  /**
   * Charge la liste des visites de monitoring depuis l'API.
   */
  protected loadVisits(): void {
    this.isLoading.set(true);
    this.ctcService.getMonitoringVisits().subscribe({
      next: (data) => {
        this.visits.set(data);
        this.isLoading.set(false);
      },
      error: (err) => {
        console.error('Erreur chargement visites monitoring', err);
        this.isLoading.set(false);
      },
    });
  }

  /**
   * Ouvre le dialog de planification de visite.
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
   * Réinitialise tous les filtres.
   */
  protected resetFilters(): void {
    this.filterType.set(null);
    this.filterStatus.set(null);
    this.selectedType = null;
    this.selectedStatus = null;
  }

  /**
   * Soumet la création d'une nouvelle visite de monitoring.
   */
  protected submitCreate(): void {
    if (this.createForm.invalid) {
      this.createForm.markAllAsTouched();
      return;
    }
    const raw = this.createForm.value;
    const payload: Partial<MonitoringVisit> = {
      studyId:     raw.studyId!,
      visitDate:   this.formatDate(raw.visitDate as unknown as Date),
      visitType:   raw.visitType!,
      monitorName: raw.monitorName!,
    };
    this.isSubmitting.set(true);
    this.ctcService.createMonitoringVisit(payload).subscribe({
      next: (created) => {
        this.visits.update(list => [created, ...list]);
        this.showCreateDialog = false;
        this.isSubmitting.set(false);
      },
      error: (err) => {
        console.error('Erreur création visite monitoring', err);
        this.isSubmitting.set(false);
      },
    });
  }

  /**
   * Retourne la sévérité PrimeNG Tag pour le statut d'une visite.
   *
   * @param visit visite de monitoring
   * @returns sévérité PrimeNG
   */
  protected getVisitStatusSeverity(visit: MonitoringVisit): 'success' | 'info' | 'secondary' | 'contrast' | 'warning' | 'danger' | undefined {
    return VISIT_STATUS_SEVERITY[visit.status] ?? 'info';
  }

  /**
   * Indique si une deadline de correction est expirée (avant aujourd'hui).
   *
   * @param deadline date ISO 8601
   * @returns vrai si la deadline est dépassée
   */
  protected isDeadlineExpired(deadline: string): boolean {
    return new Date(deadline) < new Date();
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
