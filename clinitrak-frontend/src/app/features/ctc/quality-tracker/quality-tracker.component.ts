import { Component, computed, inject, OnInit, signal } from '@angular/core';
import { CommonModule, DatePipe } from '@angular/common';
import { ReactiveFormsModule, FormsModule, FormBuilder, Validators } from '@angular/forms';
import { ButtonModule } from 'primeng/button';
import { CalendarModule } from 'primeng/calendar';
import { DialogModule } from 'primeng/dialog';
import { DropdownModule } from 'primeng/dropdown';
import { TableModule } from 'primeng/table';
import { TagModule } from 'primeng/tag';
import { InputTextareaModule } from 'primeng/inputtextarea';
import { CtcService } from '../../../core/services/ctc.service';
import {
  EVENT_STATUS_SEVERITY,
  EVENT_TYPE_OPTIONS,
  EventStatus,
  EventType,
  QualityEvent,
  Severity,
  SEVERITY_COLOR,
  SEVERITY_OPTIONS,
} from '../../../core/models/ctc.model';

/**
 * Composant suivi qualité CTC.
 *
 * <p>Affiche un en-tête avec 4 KPI (événements ouverts, CAPA en cours, EIG, déviations),
 * un tableau filtrable des événements qualité et un dialog de création.
 *
 * <p>Une bannière d'alerte rouge s'affiche si des événements critiques sont ouverts.
 * La sévérité est représentée par une pastille colorée via {@link SEVERITY_COLOR}.
 */
@Component({
  selector: 'app-quality-tracker',
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
    TableModule,
    TagModule,
    InputTextareaModule,
  ],
  template: `
    <div class="tw-space-y-6">

      <!-- En-tête -->
      <div class="tw-flex tw-items-center tw-justify-between">
        <div>
          <h2 class="tw-text-2xl tw-font-bold tw-text-gray-900">Suivi qualité</h2>
          <p class="tw-text-sm tw-text-gray-500 tw-mt-1">Déviations, EIG, CAPA et audits</p>
        </div>
        <p-button
          label="Nouvel événement"
          icon="pi pi-plus"
          severity="primary"
          (onClick)="openCreateDialog()"
        />
      </div>

      <!-- Alerte événements critiques -->
      @if (criticalEvents() > 0) {
        <div class="tw-bg-red-50 tw-border tw-border-red-300 tw-rounded-lg tw-p-4 tw-flex tw-items-center tw-gap-3">
          <i class="pi pi-exclamation-triangle tw-text-red-600 tw-text-xl tw-shrink-0"></i>
          <div>
            <p class="tw-font-semibold tw-text-red-700">
              {{ criticalEvents() }} événement(s) critique(s) ouvert(s)
            </p>
            <p class="tw-text-xs tw-text-red-600 tw-mt-0.5">
              Une action immédiate est requise.
            </p>
          </div>
        </div>
      }

      <!-- Chargement -->
      @if (isLoading()) {
        <div class="tw-flex tw-justify-center tw-py-12">
          <i class="pi pi-spin pi-spinner tw-text-4xl tw-text-blue-500"></i>
        </div>
      }

      @if (!isLoading()) {

        <!-- KPI Cards -->
        <div class="tw-grid tw-grid-cols-2 lg:tw-grid-cols-4 tw-gap-4">

          <div class="tw-bg-white tw-rounded-xl tw-border tw-border-gray-200 tw-p-4 tw-flex tw-items-center tw-gap-3 tw-shadow-sm">
            <div class="tw-w-10 tw-h-10 tw-bg-red-100 tw-rounded-full tw-flex tw-items-center tw-justify-center tw-shrink-0">
              <i class="pi pi-exclamation-circle tw-text-red-500"></i>
            </div>
            <div>
              <p class="tw-text-2xl tw-font-bold tw-text-red-600">{{ openEventsCount() }}</p>
              <p class="tw-text-xs tw-text-gray-600">Événements ouverts</p>
            </div>
          </div>

          <div class="tw-bg-white tw-rounded-xl tw-border tw-border-gray-200 tw-p-4 tw-flex tw-items-center tw-gap-3 tw-shadow-sm">
            <div class="tw-w-10 tw-h-10 tw-bg-yellow-100 tw-rounded-full tw-flex tw-items-center tw-justify-center tw-shrink-0">
              <i class="pi pi-check-circle tw-text-yellow-500"></i>
            </div>
            <div>
              <p class="tw-text-2xl tw-font-bold tw-text-yellow-600">{{ capaInProgress() }}</p>
              <p class="tw-text-xs tw-text-gray-600">CAPA en cours</p>
            </div>
          </div>

          <div class="tw-bg-white tw-rounded-xl tw-border tw-border-gray-200 tw-p-4 tw-flex tw-items-center tw-gap-3 tw-shadow-sm">
            <div class="tw-w-10 tw-h-10 tw-bg-purple-100 tw-rounded-full tw-flex tw-items-center tw-justify-center tw-shrink-0">
              <i class="pi pi-exclamation-triangle tw-text-purple-500"></i>
            </div>
            <div>
              <p class="tw-text-2xl tw-font-bold tw-text-purple-600">{{ saeCount() }}</p>
              <p class="tw-text-xs tw-text-gray-600">EIG</p>
            </div>
          </div>

          <div class="tw-bg-white tw-rounded-xl tw-border tw-border-gray-200 tw-p-4 tw-flex tw-items-center tw-gap-3 tw-shadow-sm">
            <div class="tw-w-10 tw-h-10 tw-bg-orange-100 tw-rounded-full tw-flex tw-items-center tw-justify-center tw-shrink-0">
              <i class="pi pi-flag tw-text-orange-500"></i>
            </div>
            <div>
              <p class="tw-text-2xl tw-font-bold tw-text-orange-600">{{ deviationCount() }}</p>
              <p class="tw-text-xs tw-text-gray-600">Déviations</p>
            </div>
          </div>

        </div>

        <!-- Filtres -->
        <div class="tw-flex tw-flex-wrap tw-gap-3 tw-bg-white tw-rounded-xl tw-border tw-border-gray-200 tw-p-4 tw-shadow-sm">
          <div class="tw-flex tw-flex-col tw-gap-1 tw-min-w-[180px]">
            <label class="tw-text-xs tw-font-medium tw-text-gray-600">Type d'événement</label>
            <p-dropdown
              [options]="eventTypeOptionsWithAll"
              [(ngModel)]="selectedEventType"
              optionLabel="label"
              optionValue="value"
              placeholder="Tous les types"
              [showClear]="true"
              styleClass="tw-w-full"
              (onChange)="filterEventType.set($event.value)"
            />
          </div>
          <div class="tw-flex tw-flex-col tw-gap-1 tw-min-w-[180px]">
            <label class="tw-text-xs tw-font-medium tw-text-gray-600">Sévérité</label>
            <p-dropdown
              [options]="severityOptionsWithAll"
              [(ngModel)]="selectedSeverity"
              optionLabel="label"
              optionValue="value"
              placeholder="Toutes les sévérités"
              [showClear]="true"
              styleClass="tw-w-full"
              (onChange)="filterSeverity.set($event.value)"
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

        <!-- Tableau des événements qualité -->
        <div class="tw-bg-white tw-rounded-xl tw-border tw-border-gray-200 tw-shadow-sm tw-overflow-hidden">
          <p-table
            [value]="filteredEvents()"
            [scrollable]="true"
            [paginator]="true"
            [rows]="15"
            emptyMessage="Aucun événement qualité trouvé"
            styleClass="tw-text-sm"
          >
            <ng-template pTemplate="header">
              <tr class="tw-bg-gray-50">
                <th class="tw-font-semibold tw-text-xs tw-text-gray-600 tw-px-3 tw-py-3">Étude</th>
                <th class="tw-font-semibold tw-text-xs tw-text-gray-600 tw-px-3 tw-py-3">Type</th>
                <th class="tw-font-semibold tw-text-xs tw-text-gray-600 tw-px-3 tw-py-3">Date</th>
                <th class="tw-font-semibold tw-text-xs tw-text-gray-600 tw-px-3 tw-py-3">Sévérité</th>
                <th class="tw-font-semibold tw-text-xs tw-text-gray-600 tw-px-3 tw-py-3">Statut</th>
                <th class="tw-font-semibold tw-text-xs tw-text-gray-600 tw-px-3 tw-py-3">Description</th>
                <th class="tw-font-semibold tw-text-xs tw-text-gray-600 tw-px-3 tw-py-3">Date clôture</th>
              </tr>
            </ng-template>
            <ng-template pTemplate="body" let-event>
              <tr class="hover:tw-bg-gray-50">
                <td class="tw-px-3 tw-py-3 tw-font-mono tw-text-xs tw-text-gray-600">
                  {{ event.studyId | slice:0:8 }}...
                </td>
                <td class="tw-px-3 tw-py-3 tw-text-xs">{{ event.eventTypeLabel }}</td>
                <td class="tw-px-3 tw-py-3 tw-text-xs">{{ event.eventDate | date:'dd/MM/yyyy' }}</td>
                <td class="tw-px-3 tw-py-3">
                  <span class="tw-inline-flex tw-items-center tw-gap-1.5 tw-text-xs tw-font-semibold tw-px-2 tw-py-1 tw-rounded-full"
                        [style.background-color]="getSeverityBg(event.severity)"
                        [style.color]="getSeverityColor(event.severity)">
                    <span class="tw-w-2 tw-h-2 tw-rounded-full"
                          [style.background-color]="getSeverityColor(event.severity)"></span>
                    {{ event.severityLabel }}
                  </span>
                </td>
                <td class="tw-px-3 tw-py-3">
                  <p-tag
                    [value]="event.statusLabel"
                    [severity]="getEventStatusSeverity(event)"
                  />
                </td>
                <td class="tw-px-3 tw-py-3 tw-text-xs tw-text-gray-600 tw-max-w-[200px] tw-truncate">
                  {{ event.description }}
                </td>
                <td class="tw-px-3 tw-py-3 tw-text-xs tw-text-gray-500">
                  {{ event.closureDate ? (event.closureDate | date:'dd/MM/yyyy') : '—' }}
                </td>
              </tr>
            </ng-template>
          </p-table>
        </div>

      }

      <!-- Dialog : Nouvel événement qualité -->
      <p-dialog
        header="Nouvel événement qualité"
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
              Type d'événement <span class="tw-text-red-500">*</span>
            </label>
            <p-dropdown
              formControlName="eventType"
              [options]="eventTypeOptions"
              optionLabel="label"
              optionValue="value"
              placeholder="Sélectionner le type"
              styleClass="tw-w-full"
            />
          </div>

          <div class="tw-flex tw-flex-col tw-gap-1">
            <label class="tw-text-sm tw-font-medium tw-text-gray-700">
              Date de l'événement <span class="tw-text-red-500">*</span>
            </label>
            <p-calendar
              formControlName="eventDate"
              dateFormat="dd/mm/yy"
              [showIcon]="true"
              styleClass="tw-w-full"
            />
          </div>

          <div class="tw-flex tw-flex-col tw-gap-1">
            <label class="tw-text-sm tw-font-medium tw-text-gray-700">
              Sévérité <span class="tw-text-red-500">*</span>
            </label>
            <p-dropdown
              formControlName="severity"
              [options]="severityOptions"
              optionLabel="label"
              optionValue="value"
              placeholder="Sélectionner la sévérité"
              styleClass="tw-w-full"
            />
          </div>

          <div class="tw-flex tw-flex-col tw-gap-1">
            <label class="tw-text-sm tw-font-medium tw-text-gray-700">
              Description <span class="tw-text-red-500">*</span>
            </label>
            <textarea
              pTextarea
              formControlName="description"
              rows="3"
              placeholder="Description de l'événement..."
              class="tw-w-full"
            ></textarea>
            @if (createForm.get('description')?.invalid && createForm.get('description')?.touched) {
              <span class="tw-text-xs tw-text-red-500">Ce champ est obligatoire.</span>
            }
          </div>

        </form>

        <ng-template pTemplate="footer">
          <div class="tw-flex tw-justify-end tw-gap-2">
            <p-button label="Annuler" severity="secondary" (onClick)="closeCreateDialog()" />
            <p-button
              label="Créer l'événement"
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
export class QualityTrackerComponent implements OnInit {

  private readonly ctcService = inject(CtcService);
  private readonly fb = inject(FormBuilder);

  /** Liste complète des événements qualité. */
  private readonly events = signal<QualityEvent[]>([]);

  /** Indicateur de chargement. */
  protected readonly isLoading = signal(false);

  /** Indicateur de soumission. */
  protected readonly isSubmitting = signal(false);

  /** Filtre actif sur le type d'événement. */
  protected readonly filterEventType = signal<EventType | null>(null);

  /** Filtre actif sur la sévérité. */
  protected readonly filterSeverity = signal<Severity | null>(null);

  /** Valeur liée au dropdown type (ngModel). */
  protected selectedEventType: EventType | null = null;

  /** Valeur liée au dropdown sévérité (ngModel). */
  protected selectedSeverity: Severity | null = null;

  /** Visibilité du dialog de création. */
  protected showCreateDialog = false;

  // Options
  protected readonly eventTypeOptions = EVENT_TYPE_OPTIONS;
  protected readonly severityOptions = SEVERITY_OPTIONS;
  protected readonly eventTypeOptionsWithAll = [{ label: 'Tous les types', value: null }, ...EVENT_TYPE_OPTIONS];
  protected readonly severityOptionsWithAll = [{ label: 'Toutes les sévérités', value: null }, ...SEVERITY_OPTIONS];

  /** Événements filtrés selon les signaux actifs. */
  protected readonly filteredEvents = computed(() => {
    let result = this.events();
    const type = this.filterEventType();
    const sev = this.filterSeverity();
    if (type) result = result.filter(e => e.eventType === type);
    if (sev)  result = result.filter(e => e.severity === sev);
    return result;
  });

  /** Nombre d'événements ouverts. */
  protected readonly openEventsCount = computed(() =>
    this.events().filter(e => e.status === EventStatus.OPEN || e.status === EventStatus.IN_PROGRESS).length,
  );

  /** Nombre de CAPA en cours. */
  protected readonly capaInProgress = computed(() =>
    this.events().filter(e => e.eventType === EventType.CAPA && e.status === EventStatus.IN_PROGRESS).length,
  );

  /** Nombre d'EIG. */
  protected readonly saeCount = computed(() =>
    this.events().filter(e => e.eventType === EventType.SAE).length,
  );

  /** Nombre de déviations. */
  protected readonly deviationCount = computed(() =>
    this.events().filter(e => e.eventType === EventType.DEVIATION).length,
  );

  /** Nombre d'événements critiques ouverts. */
  protected readonly criticalEvents = computed(() =>
    this.events().filter(e => e.severity === Severity.CRITICAL && e.status !== EventStatus.CLOSED && e.status !== EventStatus.CANCELLED).length,
  );

  /** Formulaire de création d'un événement qualité. */
  protected createForm = this.fb.group({
    studyId:     ['', Validators.required],
    eventType:   [null, Validators.required],
    eventDate:   [null, Validators.required],
    severity:    [null, Validators.required],
    description: ['', Validators.required],
  });

  // ─────────────────────────────────────────────────────────────

  ngOnInit(): void {
    this.loadEvents();
  }

  /**
   * Charge la liste des événements qualité depuis l'API.
   */
  protected loadEvents(): void {
    this.isLoading.set(true);
    this.ctcService.getQualityEvents().subscribe({
      next: (data) => {
        this.events.set(data);
        this.isLoading.set(false);
      },
      error: (err) => {
        console.error('Erreur chargement événements qualité', err);
        this.isLoading.set(false);
      },
    });
  }

  /**
   * Ouvre le dialog de création d'événement qualité.
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
    this.filterEventType.set(null);
    this.filterSeverity.set(null);
    this.selectedEventType = null;
    this.selectedSeverity = null;
  }

  /**
   * Soumet la création d'un nouvel événement qualité.
   */
  protected submitCreate(): void {
    if (this.createForm.invalid) {
      this.createForm.markAllAsTouched();
      return;
    }
    const raw = this.createForm.value;
    const payload: Partial<QualityEvent> = {
      studyId:     raw.studyId!,
      eventType:   raw.eventType!,
      eventDate:   this.formatDate(raw.eventDate as unknown as Date),
      severity:    raw.severity!,
      description: raw.description!,
    };
    this.isSubmitting.set(true);
    this.ctcService.createQualityEvent(payload).subscribe({
      next: (created) => {
        this.events.update(list => [created, ...list]);
        this.showCreateDialog = false;
        this.isSubmitting.set(false);
      },
      error: (err) => {
        console.error('Erreur création événement qualité', err);
        this.isSubmitting.set(false);
      },
    });
  }

  /**
   * Retourne la couleur CSS pour un niveau de sévérité.
   *
   * @param severity valeur de sévérité
   * @returns couleur hexadécimale
   */
  protected getSeverityColor(severity: Severity): string {
    return SEVERITY_COLOR[severity] ?? '#6b7280';
  }

  /**
   * Retourne la couleur de fond (opacity) pour un badge de sévérité.
   *
   * @param severity valeur de sévérité
   * @returns couleur hexadécimale claire
   */
  protected getSeverityBg(severity: Severity): string {
    const bgs: Record<string, string> = {
      LOW:      '#d1fae5',
      MEDIUM:   '#fef3c7',
      HIGH:     '#fee2e2',
      CRITICAL: '#ede9fe',
    };
    return bgs[severity] ?? '#f3f4f6';
  }

  /**
   * Retourne la sévérité PrimeNG Tag pour le statut d'un événement qualité.
   *
   * @param event événement qualité
   * @returns sévérité PrimeNG
   */
  protected getEventStatusSeverity(event: QualityEvent): 'success' | 'info' | 'secondary' | 'contrast' | 'warning' | 'danger' | undefined {
    return EVENT_STATUS_SEVERITY[event.status] ?? 'info';
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
