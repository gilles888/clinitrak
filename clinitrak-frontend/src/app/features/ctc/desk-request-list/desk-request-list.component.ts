import { Component, computed, inject, OnInit, signal } from '@angular/core';
import { CommonModule, DatePipe } from '@angular/common';
import { ReactiveFormsModule, FormBuilder, Validators } from '@angular/forms';
import { RouterLink } from '@angular/router';
import { ButtonModule } from 'primeng/button';
import { CalendarModule } from 'primeng/calendar';
import { CardModule } from 'primeng/card';
import { DialogModule } from 'primeng/dialog';
import { DropdownModule } from 'primeng/dropdown';
import { InputTextModule } from 'primeng/inputtext';
import { TagModule } from 'primeng/tag';
import { TextareaModule } from 'primeng/textarea';
import { CtcService } from '../../../core/services/ctc.service';
import {
  DESK_TYPE_OPTIONS,
  PRIORITY_OPTIONS,
  PRIORITY_SEVERITY,
  REQUEST_STATUS_SEVERITY,
  REQUEST_TYPE_OPTIONS,
  RequestStatus,
  TrialDeskRequest,
} from '../../../core/models/ctc.model';

/**
 * Composant liste des demandes guichet CTC en vue Kanban.
 *
 * <p>Affiche les demandes regroupées en trois colonnes :
 * - <strong>En attente</strong> : statut PENDING
 * - <strong>En cours</strong> : statuts ASSIGNED et IN_PROGRESS
 * - <strong>Terminées/Rejetées</strong> : statuts COMPLETED et REJECTED
 *
 * <p>Propose deux dialogs :
 * - Création d'une nouvelle demande guichet
 * - Assignation d'une demande à un agent CTC
 */
@Component({
  selector: 'app-desk-request-list',
  standalone: true,
  imports: [
    CommonModule,
    DatePipe,
    ReactiveFormsModule,
    RouterLink,
    ButtonModule,
    CalendarModule,
    CardModule,
    DialogModule,
    DropdownModule,
    InputTextModule,
    TagModule,
    TextareaModule,
  ],
  template: `
    <div class="tw-space-y-6">

      <!-- En-tête -->
      <div class="tw-flex tw-items-center tw-justify-between">
        <div>
          <h2 class="tw-text-2xl tw-font-bold tw-text-gray-900">Guichet CTC</h2>
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

      <!-- Vue Kanban -->
      @if (!isLoading()) {
        <div class="tw-grid tw-grid-cols-1 lg:tw-grid-cols-3 tw-gap-6">

          <!-- Colonne : En attente -->
          <div class="tw-flex tw-flex-col tw-gap-3">
            <div class="tw-flex tw-items-center tw-gap-2 tw-px-1">
              <span class="tw-w-3 tw-h-3 tw-rounded-full tw-bg-orange-400"></span>
              <h3 class="tw-font-semibold tw-text-gray-700">En attente</h3>
              <span class="tw-ml-auto tw-bg-orange-100 tw-text-orange-700 tw-text-xs tw-font-semibold
                           tw-px-2 tw-py-0.5 tw-rounded-full">
                {{ pendingRequests().length }}
              </span>
            </div>
            @for (req of pendingRequests(); track req.id) {
              <div class="tw-bg-white tw-rounded-xl tw-border tw-border-gray-200 tw-p-4 tw-shadow-sm
                          hover:tw-border-orange-300 tw-transition-colors">
                <ng-container *ngTemplateOutlet="requestCard; context: { $implicit: req }"></ng-container>
                <div class="tw-flex tw-gap-2 tw-mt-3 tw-pt-3 tw-border-t tw-border-gray-100">
                  <p-button
                    label="Assigner"
                    icon="pi pi-user-plus"
                    size="small"
                    severity="secondary"
                    [outlined]="true"
                    (onClick)="openAssignDialog(req)"
                    styleClass="tw-flex-1"
                  />
                </div>
              </div>
            }
            @if (pendingRequests().length === 0) {
              <div class="tw-py-8 tw-text-center tw-text-gray-400 tw-bg-gray-50 tw-rounded-xl tw-border
                          tw-border-dashed tw-border-gray-300">
                <i class="pi pi-inbox tw-text-2xl tw-block tw-mb-1"></i>
                <p class="tw-text-sm">Aucune demande en attente</p>
              </div>
            }
          </div>

          <!-- Colonne : En cours -->
          <div class="tw-flex tw-flex-col tw-gap-3">
            <div class="tw-flex tw-items-center tw-gap-2 tw-px-1">
              <span class="tw-w-3 tw-h-3 tw-rounded-full tw-bg-blue-400"></span>
              <h3 class="tw-font-semibold tw-text-gray-700">En cours</h3>
              <span class="tw-ml-auto tw-bg-blue-100 tw-text-blue-700 tw-text-xs tw-font-semibold
                           tw-px-2 tw-py-0.5 tw-rounded-full">
                {{ assignedRequests().length }}
              </span>
            </div>
            @for (req of assignedRequests(); track req.id) {
              <div class="tw-bg-white tw-rounded-xl tw-border tw-border-gray-200 tw-p-4 tw-shadow-sm
                          hover:tw-border-blue-300 tw-transition-colors">
                <ng-container *ngTemplateOutlet="requestCard; context: { $implicit: req }"></ng-container>
                @if (req.assignedTo) {
                  <div class="tw-flex tw-items-center tw-gap-1 tw-mt-2 tw-text-xs tw-text-gray-500">
                    <i class="pi pi-user"></i>
                    <span>{{ req.assignedTo }}</span>
                  </div>
                }
              </div>
            }
            @if (assignedRequests().length === 0) {
              <div class="tw-py-8 tw-text-center tw-text-gray-400 tw-bg-gray-50 tw-rounded-xl tw-border
                          tw-border-dashed tw-border-gray-300">
                <i class="pi pi-spin pi-spinner tw-text-2xl tw-block tw-mb-1"></i>
                <p class="tw-text-sm">Aucune demande en cours</p>
              </div>
            }
          </div>

          <!-- Colonne : Terminées / Rejetées -->
          <div class="tw-flex tw-flex-col tw-gap-3">
            <div class="tw-flex tw-items-center tw-gap-2 tw-px-1">
              <span class="tw-w-3 tw-h-3 tw-rounded-full tw-bg-gray-400"></span>
              <h3 class="tw-font-semibold tw-text-gray-700">Terminées / Rejetées</h3>
              <span class="tw-ml-auto tw-bg-gray-100 tw-text-gray-700 tw-text-xs tw-font-semibold
                           tw-px-2 tw-py-0.5 tw-rounded-full">
                {{ completedRequests().length }}
              </span>
            </div>
            @for (req of completedRequests(); track req.id) {
              <div class="tw-bg-white tw-rounded-xl tw-border tw-border-gray-200 tw-p-4 tw-shadow-sm tw-opacity-80">
                <ng-container *ngTemplateOutlet="requestCard; context: { $implicit: req }"></ng-container>
              </div>
            }
            @if (completedRequests().length === 0) {
              <div class="tw-py-8 tw-text-center tw-text-gray-400 tw-bg-gray-50 tw-rounded-xl tw-border
                          tw-border-dashed tw-border-gray-300">
                <i class="pi pi-check-circle tw-text-2xl tw-block tw-mb-1"></i>
                <p class="tw-text-sm">Aucune demande terminée</p>
              </div>
            }
          </div>

        </div>
      }

      <!-- Template carte Kanban -->
      <ng-template #requestCard let-req>
        <div class="tw-flex tw-items-start tw-justify-between tw-gap-2 tw-mb-2">
          <div>
            <p class="tw-text-sm tw-font-semibold tw-text-gray-800">{{ req.requestTypeLabel }}</p>
            <p class="tw-text-xs tw-text-gray-500 tw-font-mono tw-mt-0.5">{{ req.studyId | slice:0:8 }}...</p>
          </div>
          <p-tag
            [value]="req.priorityLabel"
            [severity]="getPrioritySeverity(req)"
          />
        </div>
        <p class="tw-text-xs tw-text-gray-600 tw-mb-2">{{ req.requestorName }}</p>
        <div class="tw-flex tw-items-center tw-justify-between">
          <p-tag
            [value]="req.statusLabel"
            [severity]="getStatusSeverity(req)"
          />
          @if (req.deadline) {
            <span class="tw-text-xs tw-text-gray-400">{{ req.deadline | date:'dd/MM/yy' }}</span>
          }
        </div>
      </ng-template>

      <!-- Dialog : Création demande guichet -->
      <p-dialog
        header="Nouvelle demande guichet"
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
              Type de guichet <span class="tw-text-red-500">*</span>
            </label>
            <p-dropdown
              formControlName="deskType"
              [options]="deskTypeOptions"
              optionLabel="label"
              optionValue="value"
              placeholder="Sélectionner"
              styleClass="tw-w-full"
            />
          </div>

          <div class="tw-flex tw-flex-col tw-gap-1">
            <label class="tw-text-sm tw-font-medium tw-text-gray-700">
              Type de demande <span class="tw-text-red-500">*</span>
            </label>
            <p-dropdown
              formControlName="requestType"
              [options]="requestTypeOptions"
              optionLabel="label"
              optionValue="value"
              placeholder="Sélectionner"
              styleClass="tw-w-full"
            />
          </div>

          <div class="tw-flex tw-flex-col tw-gap-1">
            <label class="tw-text-sm tw-font-medium tw-text-gray-700">
              Nom du demandeur <span class="tw-text-red-500">*</span>
            </label>
            <input pInputText formControlName="requestorName" placeholder="Nom complet" class="tw-w-full" />
            @if (createForm.get('requestorName')?.invalid && createForm.get('requestorName')?.touched) {
              <span class="tw-text-xs tw-text-red-500">Ce champ est obligatoire.</span>
            }
          </div>

          <div class="tw-flex tw-flex-col tw-gap-1">
            <label class="tw-text-sm tw-font-medium tw-text-gray-700">
              Email demandeur <span class="tw-text-red-500">*</span>
            </label>
            <input pInputText formControlName="requestorEmail" type="email" placeholder="email@domaine.com" class="tw-w-full" />
            @if (createForm.get('requestorEmail')?.invalid && createForm.get('requestorEmail')?.touched) {
              <span class="tw-text-xs tw-text-red-500">Email invalide.</span>
            }
          </div>

          <div class="tw-flex tw-flex-col tw-gap-1">
            <label class="tw-text-sm tw-font-medium tw-text-gray-700">Organisation</label>
            <input pInputText formControlName="requestorOrganization" placeholder="Service / Institution" class="tw-w-full" />
          </div>

          <div class="tw-flex tw-flex-col tw-gap-1">
            <label class="tw-text-sm tw-font-medium tw-text-gray-700">Notes</label>
            <textarea pTextarea formControlName="notes" rows="3" placeholder="Informations complémentaires..." class="tw-w-full"></textarea>
          </div>

        </form>

        <ng-template pTemplate="footer">
          <div class="tw-flex tw-justify-end tw-gap-2">
            <p-button label="Annuler" severity="secondary" (onClick)="closeCreateDialog()" />
            <p-button
              label="Créer la demande"
              icon="pi pi-check"
              [loading]="isSubmitting()"
              [disabled]="createForm.invalid"
              (onClick)="submitCreate()"
            />
          </div>
        </ng-template>
      </p-dialog>

      <!-- Dialog : Assignation -->
      <p-dialog
        header="Assigner la demande"
        [(visible)]="showAssignDialog"
        [modal]="true"
        [style]="{ width: '420px' }"
        [draggable]="false"
      >
        <form [formGroup]="assignForm" class="tw-space-y-4 tw-pt-2">

          <div class="tw-flex tw-flex-col tw-gap-1">
            <label class="tw-text-sm tw-font-medium tw-text-gray-700">
              Assigner à <span class="tw-text-red-500">*</span>
            </label>
            <input pInputText formControlName="assignedTo" placeholder="Nom de l'agent CTC" class="tw-w-full" />
            @if (assignForm.get('assignedTo')?.invalid && assignForm.get('assignedTo')?.touched) {
              <span class="tw-text-xs tw-text-red-500">Ce champ est obligatoire.</span>
            }
          </div>

          <div class="tw-flex tw-flex-col tw-gap-1">
            <label class="tw-text-sm tw-font-medium tw-text-gray-700">
              Priorité <span class="tw-text-red-500">*</span>
            </label>
            <p-dropdown
              formControlName="priority"
              [options]="priorityOptions"
              optionLabel="label"
              optionValue="value"
              placeholder="Sélectionner la priorité"
              styleClass="tw-w-full"
            />
          </div>

          <div class="tw-flex tw-flex-col tw-gap-1">
            <label class="tw-text-sm tw-font-medium tw-text-gray-700">Deadline</label>
            <p-calendar
              formControlName="deadline"
              dateFormat="dd/mm/yy"
              [showIcon]="true"
              styleClass="tw-w-full"
            />
          </div>

        </form>

        <ng-template pTemplate="footer">
          <div class="tw-flex tw-justify-end tw-gap-2">
            <p-button label="Annuler" severity="secondary" (onClick)="closeAssignDialog()" />
            <p-button
              label="Assigner"
              icon="pi pi-check"
              [loading]="isSubmitting()"
              [disabled]="assignForm.invalid"
              (onClick)="submitAssign()"
            />
          </div>
        </ng-template>
      </p-dialog>

    </div>
  `,
})
export class DeskRequestListComponent implements OnInit {

  private readonly ctcService = inject(CtcService);
  private readonly fb = inject(FormBuilder);

  /** Liste complète des demandes guichet. */
  protected readonly requests = signal<TrialDeskRequest[]>([]);

  /** Indicateur de chargement. */
  protected readonly isLoading = signal(false);

  /** Indicateur de soumission d'un formulaire. */
  protected readonly isSubmitting = signal(false);

  /** Visibilité du dialog de création. */
  protected showCreateDialog = false;

  /** Visibilité du dialog d'assignation. */
  protected showAssignDialog = false;

  /** Identifiant de la demande en cours d'assignation. */
  private selectedRequestId = signal<string | null>(null);

  // Options de dropdowns
  protected readonly deskTypeOptions = DESK_TYPE_OPTIONS;
  protected readonly requestTypeOptions = REQUEST_TYPE_OPTIONS;
  protected readonly priorityOptions = PRIORITY_OPTIONS;

  /** Demandes en statut PENDING. */
  protected readonly pendingRequests = computed(() =>
    this.requests().filter(r => r.status === RequestStatus.PENDING),
  );

  /** Demandes en statut ASSIGNED ou IN_PROGRESS. */
  protected readonly assignedRequests = computed(() =>
    this.requests().filter(r => r.status === RequestStatus.ASSIGNED || r.status === RequestStatus.IN_PROGRESS),
  );

  /** Demandes en statut COMPLETED ou REJECTED. */
  protected readonly completedRequests = computed(() =>
    this.requests().filter(r => r.status === RequestStatus.COMPLETED || r.status === RequestStatus.REJECTED),
  );

  /** Formulaire de création d'une demande guichet. */
  protected createForm = this.fb.group({
    studyId:               ['', Validators.required],
    deskType:              [null, Validators.required],
    requestType:           [null, Validators.required],
    requestorName:         ['', Validators.required],
    requestorEmail:        ['', [Validators.required, Validators.email]],
    requestorOrganization: [''],
    notes:                 [''],
  });

  /** Formulaire d'assignation d'une demande guichet. */
  protected assignForm = this.fb.group({
    assignedTo: ['', Validators.required],
    priority:   [null, Validators.required],
    deadline:   [null],
  });

  // ─────────────────────────────────────────────────────────────

  ngOnInit(): void {
    this.loadRequests();
  }

  /**
   * Charge la liste des demandes guichet depuis l'API.
   */
  protected loadRequests(): void {
    this.isLoading.set(true);
    this.ctcService.getDeskRequests().subscribe({
      next: (data) => {
        this.requests.set(data);
        this.isLoading.set(false);
      },
      error: (err) => {
        console.error('Erreur chargement demandes guichet', err);
        this.isLoading.set(false);
      },
    });
  }

  /**
   * Ouvre le dialog de création de demande.
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
   * Ouvre le dialog d'assignation pour la demande fournie.
   *
   * @param req demande guichet à assigner
   */
  protected openAssignDialog(req: TrialDeskRequest): void {
    this.selectedRequestId.set(req.id);
    this.assignForm.reset();
    this.showAssignDialog = true;
  }

  /**
   * Ferme le dialog d'assignation.
   */
  protected closeAssignDialog(): void {
    this.showAssignDialog = false;
    this.selectedRequestId.set(null);
  }

  /**
   * Soumet la création d'une nouvelle demande guichet.
   */
  protected submitCreate(): void {
    if (this.createForm.invalid) {
      this.createForm.markAllAsTouched();
      return;
    }
    this.isSubmitting.set(true);
    this.ctcService.createDeskRequest(this.createForm.value as Partial<TrialDeskRequest>).subscribe({
      next: (created) => {
        this.requests.update(list => [created, ...list]);
        this.showCreateDialog = false;
        this.isSubmitting.set(false);
      },
      error: (err) => {
        console.error('Erreur création demande guichet', err);
        this.isSubmitting.set(false);
      },
    });
  }

  /**
   * Soumet l'assignation d'une demande guichet.
   */
  protected submitAssign(): void {
    if (this.assignForm.invalid) {
      this.assignForm.markAllAsTouched();
      return;
    }
    const id = this.selectedRequestId();
    if (!id) return;

    const raw = this.assignForm.value;
    const deadline = raw.deadline
      ? this.formatDate(raw.deadline as unknown as Date)
      : undefined;

    this.isSubmitting.set(true);
    this.ctcService.assignDeskRequest(id, {
      assignedTo: raw.assignedTo!,
      priority: raw.priority!,
      deadline,
    }).subscribe({
      next: (updated) => {
        this.requests.update(list => list.map(r => r.id === updated.id ? updated : r));
        this.showAssignDialog = false;
        this.isSubmitting.set(false);
      },
      error: (err) => {
        console.error('Erreur assignation demande guichet', err);
        this.isSubmitting.set(false);
      },
    });
  }

  /**
   * Retourne la sévérité PrimeNG Tag pour le statut d'une demande.
   *
   * @param req demande guichet
   * @returns sévérité PrimeNG
   */
  protected getStatusSeverity(req: TrialDeskRequest): string {
    return REQUEST_STATUS_SEVERITY[req.status] ?? 'info';
  }

  /**
   * Retourne la sévérité PrimeNG Tag pour la priorité d'une demande.
   *
   * @param req demande guichet
   * @returns sévérité PrimeNG
   */
  protected getPrioritySeverity(req: TrialDeskRequest): string {
    return PRIORITY_SEVERITY[req.priority] ?? 'info';
  }

  /**
   * Formate un objet Date en chaîne ISO 8601 (yyyy-MM-dd).
   *
   * @param date valeur issue du calendrier PrimeNG
   * @returns chaîne ISO 8601
   */
  private formatDate(date: Date): string {
    const y = date.getFullYear();
    const m = String(date.getMonth() + 1).padStart(2, '0');
    const d = String(date.getDate()).padStart(2, '0');
    return `${y}-${m}-${d}`;
  }
}
