import { Component, computed, inject, OnInit, signal } from '@angular/core';
import { CommonModule, DatePipe } from '@angular/common';
import { ButtonModule } from 'primeng/button';
import { CardModule } from 'primeng/card';
import { DialogModule } from 'primeng/dialog';
import { InputTextModule } from 'primeng/inputtext';
import { TableModule } from 'primeng/table';
import { TagModule } from 'primeng/tag';
import { InputTextareaModule } from 'primeng/inputtextarea';
import { PharmacyService } from '../../../core/services/pharmacy.service';
import { EmergencyUnblinding } from '../../../core/models/pharmacy.model';

/**
 * Composant de gestion des levées d'aveugle d'urgence.
 *
 * <p>Affiche l'historique des levées d'aveugle enregistrées et propose un dialog
 * en deux étapes pour soumettre une nouvelle demande :
 * <ol>
 *   <li>Saisie des informations (étude, patient, demandeur, raison médicale)</li>
 *   <li>Confirmation avec avertissement de caractère irréversible</li>
 * </ol>
 *
 * <p>Après soumission réussie, le traitement révélé est affiché si présent dans
 * la réponse de l'API.
 *
 * @remarks Action irréversible enregistrée dans l'audit trail réglementaire.
 */
@Component({
  selector: 'app-emergency-unblinding',
  standalone: true,
  imports: [
    CommonModule,
    DatePipe,
    ButtonModule,
    CardModule,
    DialogModule,
    InputTextModule,
    TableModule,
    TagModule,
    InputTextareaModule,
  ],
  template: `
    <div class="tw-space-y-6">

      <!-- En-tête -->
      <div class="tw-flex tw-items-center tw-justify-between">
        <div>
          <h2 class="tw-text-2xl tw-font-bold tw-text-gray-900">Levées d'aveugle d'urgence</h2>
          <p class="tw-text-sm tw-text-red-600 tw-mt-1 tw-font-medium">
            <i class="pi pi-exclamation-triangle tw-mr-1"></i>
            Actions irréversibles enregistrées dans l'audit réglementaire
          </p>
        </div>
        <button
          pButton
          label="Nouvelle levée d'aveugle"
          icon="pi pi-lock-open"
          severity="danger"
          (click)="openDialog()"
        ></button>
      </div>

      <!-- Traitement révélé (après soumission) -->
      @if (revealedTreatment()) {
        <div class="tw-p-4 tw-bg-blue-50 tw-border tw-border-blue-300 tw-rounded-lg">
          <div class="tw-flex tw-items-center tw-gap-2 tw-mb-1">
            <i class="pi pi-info-circle tw-text-blue-600"></i>
            <span class="tw-font-semibold tw-text-blue-800">Traitement révélé</span>
          </div>
          <p class="tw-text-blue-900 tw-text-lg tw-font-bold">{{ revealedTreatment() }}</p>
        </div>
      }

      <!-- Historique des levées -->
      @if (isLoading()) {
        <div class="tw-flex tw-justify-center tw-py-12">
          <i class="pi pi-spin pi-spinner tw-text-4xl tw-text-blue-500"></i>
        </div>
      } @else {
        <p-card styleClass="tw-border tw-border-gray-100">
          <ng-template pTemplate="header">
            <div class="tw-px-4 tw-pt-4">
              <h3 class="tw-font-semibold tw-text-gray-800">Historique des levées d'aveugle</h3>
            </div>
          </ng-template>
          <p-table
            [value]="unblindingHistory()"
            styleClass="tw-text-sm"
            [rows]="10"
            [paginator]="true"
          >
            <ng-template pTemplate="header">
              <tr class="tw-bg-gray-50">
                <th class="tw-text-xs tw-font-semibold tw-text-gray-600">Étude</th>
                <th class="tw-text-xs tw-font-semibold tw-text-gray-600">Patient</th>
                <th class="tw-text-xs tw-font-semibold tw-text-gray-600">Demandeur</th>
                <th class="tw-text-xs tw-font-semibold tw-text-gray-600">Raison</th>
                <th class="tw-text-xs tw-font-semibold tw-text-gray-600">Date</th>
                <th class="tw-text-xs tw-font-semibold tw-text-gray-600">Traitement</th>
              </tr>
            </ng-template>
            <ng-template pTemplate="body" let-u>
              <tr class="hover:tw-bg-gray-50">
                <td class="tw-px-3 tw-py-2 tw-font-mono tw-text-xs tw-text-gray-500">
                  {{ u.studyId | slice:0:8 }}…
                </td>
                <td class="tw-px-3 tw-py-2 tw-font-semibold">{{ u.patientCode }}</td>
                <td class="tw-px-3 tw-py-2 tw-text-xs tw-text-gray-600">{{ u.requestedBy }}</td>
                <td class="tw-px-3 tw-py-2 tw-text-xs tw-text-gray-700 tw-max-w-xs tw-truncate">
                  {{ u.reason }}
                </td>
                <td class="tw-px-3 tw-py-2 tw-text-xs">{{ u.requestDate | date:'dd/MM/yyyy HH:mm' }}</td>
                <td class="tw-px-3 tw-py-2">
                  @if (u.treatment) {
                    <span class="tw-text-sm tw-font-bold tw-text-blue-700">{{ u.treatment }}</span>
                  } @else {
                    <span class="tw-text-xs tw-text-gray-400">—</span>
                  }
                </td>
              </tr>
            </ng-template>
            <ng-template pTemplate="emptymessage">
              <tr>
                <td colspan="6" class="tw-py-10 tw-text-center tw-text-gray-400">
                  <i class="pi pi-lock tw-text-3xl tw-block tw-mb-2"></i>
                  <p>Aucune levée d'aveugle enregistrée</p>
                </td>
              </tr>
            </ng-template>
          </p-table>
        </p-card>
      }

    </div>

    <!-- Dialog de demande de levée d'aveugle -->
    <p-dialog
      [(visible)]="showDialog"
      [modal]="true"
      [style]="{ width: '500px' }"
      header="Levée d'aveugle d'urgence"
      (onHide)="resetDialog()"
    >
      @if (!showConfirmStep()) {
        <!-- Étape 1 : formulaire -->
        <div class="tw-space-y-4">
          <div>
            <label class="tw-block tw-text-sm tw-font-medium tw-mb-1">
              ID Étude <span class="tw-text-red-500">*</span>
            </label>
            <input
              pInputText
              [value]="studyId()"
              (input)="studyId.set($any($event.target).value)"
              class="tw-w-full"
              placeholder="UUID de l'étude"
            />
          </div>
          <div>
            <label class="tw-block tw-text-sm tw-font-medium tw-mb-1">
              Code Patient <span class="tw-text-red-500">*</span>
            </label>
            <input
              pInputText
              [value]="patientCode()"
              (input)="patientCode.set($any($event.target).value)"
              class="tw-w-full"
              placeholder="Ex : PAT-001"
            />
          </div>
          <div>
            <label class="tw-block tw-text-sm tw-font-medium tw-mb-1">
              Demandeur <span class="tw-text-red-500">*</span>
            </label>
            <input
              pInputText
              [value]="requestedBy()"
              (input)="requestedBy.set($any($event.target).value)"
              class="tw-w-full"
              placeholder="Nom du médecin responsable"
            />
          </div>
          <div>
            <label class="tw-block tw-text-sm tw-font-medium tw-mb-1">
              Raison médicale <span class="tw-text-red-500">*</span>
            </label>
            <textarea
              pTextarea
              rows="3"
              [value]="reason()"
              (input)="reason.set($any($event.target).value)"
              class="tw-w-full"
              placeholder="Situation d'urgence médicale justifiant la levée…"
            ></textarea>
          </div>
          <button
            pButton
            label="Suivant"
            icon="pi pi-arrow-right"
            iconPos="right"
            [disabled]="!canProceed()"
            (click)="showConfirmStep.set(true)"
            class="tw-w-full"
          ></button>
        </div>
      } @else {
        <!-- Étape 2 : confirmation sécurité -->
        <div class="tw-space-y-4">
          <div class="tw-p-4 tw-bg-red-50 tw-border tw-border-red-300 tw-rounded-lg">
            <div class="tw-flex tw-items-center tw-gap-2 tw-mb-2">
              <i class="pi pi-exclamation-triangle tw-text-red-600 tw-text-xl"></i>
              <span class="tw-font-bold tw-text-red-700 tw-uppercase tw-tracking-wide">
                ATTENTION — Action irréversible
              </span>
            </div>
            <p class="tw-text-sm tw-text-red-700">
              Cette levée d'aveugle pour le patient
              <strong>{{ patientCode() }}</strong>
              (étude <span class="tw-font-mono tw-text-xs">{{ studyId() | slice:0:8 }}…</span>)
              sera enregistrée définitivement dans l'audit trail réglementaire.
              Confirmez-vous cette action ?
            </p>
          </div>

          <!-- Récapitulatif -->
          <div class="tw-bg-gray-50 tw-rounded tw-p-3 tw-text-sm tw-space-y-1">
            <p><span class="tw-text-gray-500">Demandeur :</span> <strong>{{ requestedBy() }}</strong></p>
            <p><span class="tw-text-gray-500">Raison :</span> {{ reason() }}</p>
          </div>

          <div class="tw-flex tw-gap-2">
            <button
              pButton
              label="Retour"
              severity="secondary"
              (click)="showConfirmStep.set(false)"
              class="tw-flex-1"
            ></button>
            <button
              pButton
              label="Confirmer la levée"
              severity="danger"
              icon="pi pi-lock-open"
              [loading]="isSubmitting()"
              (click)="submit()"
              class="tw-flex-1"
            ></button>
          </div>
        </div>
      }
    </p-dialog>
  `,
})
export class EmergencyUnblindingComponent implements OnInit {

  private readonly pharmacyService = inject(PharmacyService);

  /** Indicateur de chargement de l'historique. */
  protected readonly isLoading = signal(false);

  /** Historique des levées d'aveugle. */
  protected readonly unblindingHistory = signal<EmergencyUnblinding[]>([]);

  /** Visibilité du dialog. */
  protected showDialog = false;

  /** Indicateur de l'étape de confirmation. */
  protected readonly showConfirmStep = signal(false);

  /** Traitement révélé après soumission réussie. */
  protected readonly revealedTreatment = signal<string | null>(null);

  /** Indicateur de soumission en cours. */
  protected readonly isSubmitting = signal(false);

  // Champs du formulaire (signaux)
  /** ID de l'étude saisi dans le formulaire. */
  protected readonly studyId = signal('');
  /** Code patient saisi dans le formulaire. */
  protected readonly patientCode = signal('');
  /** Demandeur saisi dans le formulaire. */
  protected readonly requestedBy = signal('');
  /** Raison médicale saisie dans le formulaire. */
  protected readonly reason = signal('');

  /**
   * Indique si tous les champs obligatoires sont remplis pour passer à l'étape de confirmation.
   */
  protected readonly canProceed = computed(() =>
    this.studyId().trim().length > 0 &&
    this.patientCode().trim().length > 0 &&
    this.requestedBy().trim().length > 0 &&
    this.reason().trim().length > 5,
  );

  // ─────────────────────────────────────────────────────────────

  ngOnInit(): void {
    this.loadHistory();
  }

  /**
   * Initialise l'historique des levées d'aveugle.
   *
   * <p>L'historique est alimenté au fil des soumissions durant la session courante.
   * Une API GET dédiée pourrait être ajoutée côté pharmacy-service pour la persistance.
   */
  private loadHistory(): void {
    // L'historique est géré localement via le signal unblindingHistory.
    // Les nouvelles soumissions sont ajoutées en tête de liste lors de submit().
    this.isLoading.set(false);
  }

  /**
   * Ouvre le dialog de demande de levée d'aveugle.
   */
  protected openDialog(): void {
    this.showDialog = true;
  }

  /**
   * Remet à zéro les champs du dialog.
   */
  protected resetDialog(): void {
    this.showConfirmStep.set(false);
    this.studyId.set('');
    this.patientCode.set('');
    this.requestedBy.set('');
    this.reason.set('');
  }

  /**
   * Soumet la demande de levée d'aveugle à l'API.
   *
   * <p>En cas de succès, affiche le traitement révélé si disponible
   * et ajoute la levée à l'historique local.
   */
  protected submit(): void {
    if (!this.canProceed()) return;
    this.isSubmitting.set(true);

    const payload: Record<string, unknown> = {
      studyId:     this.studyId(),
      patientCode: this.patientCode(),
      requestedBy: this.requestedBy(),
      reason:      this.reason(),
    };

    this.pharmacyService.requestEmergencyUnblinding(payload).subscribe({
      next: result => {
        this.isSubmitting.set(false);
        this.showDialog = false;
        if (result.treatment) {
          this.revealedTreatment.set(result.treatment);
        }
        this.unblindingHistory.update(list => [result, ...list]);
        this.resetDialog();
      },
      error: err => {
        console.error('Erreur levée d\'aveugle', err);
        this.isSubmitting.set(false);
      },
    });
  }
}
