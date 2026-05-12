import { Component, computed, inject, signal } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { Router, RouterLink } from '@angular/router';
import { StepsModule } from 'primeng/steps';
import { CardModule } from 'primeng/card';
import { DropdownModule } from 'primeng/dropdown';
import { InputTextModule } from 'primeng/inputtext';
import { InputNumberModule } from 'primeng/inputnumber';
import { InputTextareaModule } from 'primeng/inputtextarea';
import { CheckboxModule } from 'primeng/checkbox';
import { ButtonModule } from 'primeng/button';
import { MessagesModule } from 'primeng/messages';
import { Message } from 'primeng/api';
import { ExchangeService } from '../../../core/services/exchange.service';
import {
  ExchangeRequestType,
  REQUEST_TYPE_OPTIONS,
  TARGET_MODULE_OPTIONS,
  TargetModule,
} from '../../../core/models/exchange.model';

/** Options de phase d'étude. */
const STUDY_PHASE_OPTIONS = [
  { label: 'Phase I',   value: 'PHASE_I' },
  { label: 'Phase II',  value: 'PHASE_II' },
  { label: 'Phase III', value: 'PHASE_III' },
  { label: 'Phase IV',  value: 'PHASE_IV' },
  { label: 'Non applicable', value: 'NA' },
];

/**
 * Assistant de soumission en 6 étapes pour le portail Exchange.
 *
 * <p>Guide l'utilisateur externe à travers la création d'une demande :
 * type → protocole → équipe → documents → déclarations → validation.
 */
@Component({
  selector: 'app-request-wizard',
  standalone: true,
  imports: [
    ReactiveFormsModule,
    RouterLink,
    StepsModule,
    CardModule,
    DropdownModule,
    InputTextModule,
    InputNumberModule,
    InputTextareaModule,
    CheckboxModule,
    ButtonModule,
    MessagesModule,
  ],
  template: `
    <div class="tw-min-h-screen tw-bg-gray-50 tw-p-6">
      <div class="tw-max-w-3xl tw-mx-auto">

        <!-- Titre -->
        <div class="tw-mb-8">
          <a routerLink="/exchange" class="tw-text-blue-600 hover:tw-underline tw-text-sm">
            &larr; Retour à l'accueil Exchange
          </a>
          <h1 class="tw-text-2xl tw-font-bold tw-text-gray-900 tw-mt-2">Nouvelle demande</h1>
        </div>

        <!-- Steps -->
        <p-steps [model]="steps" [activeIndex]="activeStep()" styleClass="tw-mb-8" [readonly]="true" />

        <!-- Messages -->
        @if (messages().length > 0) {
          <p-messages [value]="messages()" styleClass="tw-mb-4" />
        }

        <!-- Contenu des étapes -->
        <div class="tw-bg-white tw-rounded-xl tw-shadow tw-p-8">

          <!-- Étape 1 : Type de demande -->
          @if (activeStep() === 0) {
            <h2 class="tw-text-xl tw-font-semibold tw-text-gray-800 tw-mb-6">
              Étape 1 — Type de demande
            </h2>

            <div class="tw-mb-6">
              <p class="tw-text-sm tw-font-medium tw-text-gray-700 tw-mb-3">Module cible *</p>
              <div class="tw-grid tw-grid-cols-2 tw-gap-4">
                @for (opt of targetModuleOptions; track opt.value) {
                  <div
                    class="tw-border-2 tw-rounded-xl tw-p-4 tw-cursor-pointer tw-transition-all"
                    [class.tw-border-blue-500]="wizardData()['targetModule'] === opt.value"
                    [class.tw-border-gray-200]="wizardData()['targetModule'] !== opt.value"
                    [class.tw-bg-blue-50]="wizardData()['targetModule'] === opt.value"
                    (click)="setWizardField('targetModule', opt.value)"
                  >
                    <p class="tw-font-semibold tw-text-gray-800">{{ opt.label }}</p>
                  </div>
                }
              </div>
            </div>

            <div class="tw-mb-6">
              <p class="tw-text-sm tw-font-medium tw-text-gray-700 tw-mb-3">Type de demande *</p>
              <div class="tw-grid tw-grid-cols-2 tw-gap-4">
                @for (opt of requestTypeOptions; track opt.value) {
                  <div
                    class="tw-border-2 tw-rounded-xl tw-p-4 tw-cursor-pointer tw-transition-all"
                    [class.tw-border-blue-500]="wizardData()['requestType'] === opt.value"
                    [class.tw-border-gray-200]="wizardData()['requestType'] !== opt.value"
                    [class.tw-bg-blue-50]="wizardData()['requestType'] === opt.value"
                    (click)="setWizardField('requestType', opt.value)"
                  >
                    <p class="tw-font-semibold tw-text-gray-800">{{ opt.label }}</p>
                  </div>
                }
              </div>
            </div>
          }

          <!-- Étape 2 : Protocole -->
          @if (activeStep() === 1) {
            <h2 class="tw-text-xl tw-font-semibold tw-text-gray-800 tw-mb-6">
              Étape 2 — Informations sur le protocole
            </h2>
            <form [formGroup]="protocolForm" class="tw-space-y-5">
              <div class="tw-flex tw-flex-col tw-gap-1.5">
                <label for="title" class="tw-text-sm tw-font-medium tw-text-gray-700">Titre de l'étude *</label>
                <input pInputText id="title" formControlName="title" placeholder="Titre complet de l'étude" class="tw-w-full" />
                @if (protocolForm.controls.title.dirty && protocolForm.controls.title.errors?.['required']) {
                  <small class="tw-text-red-500">Obligatoire</small>
                }
              </div>
              <div class="tw-flex tw-flex-col tw-gap-1.5">
                <label for="description" class="tw-text-sm tw-font-medium tw-text-gray-700">Description</label>
                <textarea
                  pInputTextarea
                  id="description"
                  formControlName="description"
                  placeholder="Résumé du protocole..."
                  rows="4"
                  class="tw-w-full"
                ></textarea>
              </div>
              <div class="tw-grid tw-grid-cols-2 tw-gap-4">
                <div class="tw-flex tw-flex-col tw-gap-1.5">
                  <label for="studyPhase" class="tw-text-sm tw-font-medium tw-text-gray-700">Phase</label>
                  <p-dropdown
                    inputId="studyPhase"
                    formControlName="studyPhase"
                    [options]="studyPhaseOptions"
                    optionLabel="label"
                    optionValue="value"
                    placeholder="Sélectionner"
                    styleClass="tw-w-full"
                  />
                </div>
                <div class="tw-flex tw-flex-col tw-gap-1.5">
                  <label for="expectedPatients" class="tw-text-sm tw-font-medium tw-text-gray-700">Patients prévus</label>
                  <p-inputNumber
                    inputId="expectedPatients"
                    formControlName="expectedPatients"
                    [min]="1"
                    placeholder="Nombre"
                    styleClass="tw-w-full"
                    inputStyleClass="tw-w-full"
                  />
                </div>
              </div>
            </form>
          }

          <!-- Étape 3 : Équipe -->
          @if (activeStep() === 2) {
            <h2 class="tw-text-xl tw-font-semibold tw-text-gray-800 tw-mb-6">
              Étape 3 — Équipe de recherche
            </h2>
            <form [formGroup]="teamForm" class="tw-space-y-5">
              <div class="tw-flex tw-flex-col tw-gap-1.5">
                <label for="principalInvestigator" class="tw-text-sm tw-font-medium tw-text-gray-700">Investigateur principal *</label>
                <input pInputText id="principalInvestigator" formControlName="principalInvestigator" placeholder="Dr. Prénom Nom" class="tw-w-full" />
                @if (teamForm.controls.principalInvestigator.dirty && teamForm.controls.principalInvestigator.errors?.['required']) {
                  <small class="tw-text-red-500">Obligatoire</small>
                }
              </div>
              <div class="tw-flex tw-flex-col tw-gap-1.5">
                <label for="institution" class="tw-text-sm tw-font-medium tw-text-gray-700">Institution *</label>
                <input pInputText id="institution" formControlName="institution" placeholder="Nom de l'institution" class="tw-w-full" />
                @if (teamForm.controls.institution.dirty && teamForm.controls.institution.errors?.['required']) {
                  <small class="tw-text-red-500">Obligatoire</small>
                }
              </div>
              <div class="tw-flex tw-flex-col tw-gap-1.5">
                <label for="coInvestigators" class="tw-text-sm tw-font-medium tw-text-gray-700">Co-investigateurs</label>
                <input pInputText id="coInvestigators" formControlName="coInvestigators" placeholder="Séparés par des virgules" class="tw-w-full" />
                <small class="tw-text-gray-400">Entrez plusieurs noms séparés par des virgules</small>
              </div>
            </form>
          }

          <!-- Étape 4 : Documents -->
          @if (activeStep() === 3) {
            <h2 class="tw-text-xl tw-font-semibold tw-text-gray-800 tw-mb-6">
              Étape 4 — Documents
            </h2>
            <div class="tw-border-2 tw-border-dashed tw-border-gray-300 tw-rounded-xl tw-p-8 tw-text-center">
              <i class="pi pi-upload tw-text-4xl tw-text-gray-400 tw-mb-3 tw-block"></i>
              <p class="tw-text-gray-600 tw-mb-4">Glissez vos fichiers ici ou cliquez pour sélectionner</p>
              <input
                type="file"
                multiple
                (change)="onFilesSelected($event)"
                class="tw-hidden"
                #fileInput
              />
              <p-button
                label="Sélectionner des fichiers"
                icon="pi pi-folder-open"
                severity="secondary"
                (onClick)="fileInput.click()"
              />
            </div>

            @if (selectedFiles().length > 0) {
              <div class="tw-mt-4 tw-space-y-2">
                <p class="tw-text-sm tw-font-medium tw-text-gray-700">Fichiers sélectionnés :</p>
                @for (file of selectedFiles(); track file.name) {
                  <div class="tw-flex tw-items-center tw-justify-between tw-bg-gray-50 tw-rounded-lg tw-px-4 tw-py-2">
                    <div class="tw-flex tw-items-center tw-gap-2">
                      <i class="pi pi-file tw-text-blue-500"></i>
                      <span class="tw-text-sm tw-text-gray-700">{{ file.name }}</span>
                    </div>
                    <span class="tw-text-xs tw-text-gray-400">{{ formatFileSize(file.size) }}</span>
                  </div>
                }
              </div>
            }
          }

          <!-- Étape 5 : Déclarations -->
          @if (activeStep() === 4) {
            <h2 class="tw-text-xl tw-font-semibold tw-text-gray-800 tw-mb-6">
              Étape 5 — Déclarations
            </h2>
            <form [formGroup]="declarationsForm" class="tw-space-y-4">
              <div class="tw-flex tw-items-start tw-gap-3 tw-p-4 tw-bg-gray-50 tw-rounded-xl">
                <p-checkbox
                  formControlName="rgpdCompliance"
                  [binary]="true"
                  inputId="rgpdCompliance"
                />
                <label for="rgpdCompliance" class="tw-text-sm tw-text-gray-700 tw-cursor-pointer">
                  Je confirme que cette étude respecte le Règlement Général sur la Protection des Données (RGPD)
                  et les législations applicables en matière de protection des données personnelles.
                </label>
              </div>
              <div class="tw-flex tw-items-start tw-gap-3 tw-p-4 tw-bg-gray-50 tw-rounded-xl">
                <p-checkbox
                  formControlName="institutionAuthorization"
                  [binary]="true"
                  inputId="institutionAuthorization"
                />
                <label for="institutionAuthorization" class="tw-text-sm tw-text-gray-700 tw-cursor-pointer">
                  Je déclare avoir obtenu l'autorisation de mon institution pour soumettre cette demande
                  et mener cette recherche dans les locaux concernés.
                </label>
              </div>
              <div class="tw-flex tw-items-start tw-gap-3 tw-p-4 tw-bg-gray-50 tw-rounded-xl">
                <p-checkbox
                  formControlName="noConflictOfInterest"
                  [binary]="true"
                  inputId="noConflictOfInterest"
                />
                <label for="noConflictOfInterest" class="tw-text-sm tw-text-gray-700 tw-cursor-pointer">
                  Je déclare l'absence de conflit d'intérêt susceptible d'influencer les résultats
                  ou la conduite de cette étude.
                </label>
              </div>
            </form>
          }

          <!-- Étape 6 : Validation / Résumé -->
          @if (activeStep() === 5) {
            <h2 class="tw-text-xl tw-font-semibold tw-text-gray-800 tw-mb-6">
              Étape 6 — Récapitulatif et validation
            </h2>
            <div class="tw-space-y-4">

              <div class="tw-bg-gray-50 tw-rounded-xl tw-p-4">
                <p class="tw-text-xs tw-text-gray-500 tw-uppercase tw-font-semibold tw-mb-3">Type de demande</p>
                <div class="tw-grid tw-grid-cols-2 tw-gap-2 tw-text-sm">
                  <span class="tw-text-gray-500">Module cible :</span>
                  <span class="tw-font-medium">{{ getModuleLabel(wizardData()['targetModule']) }}</span>
                  <span class="tw-text-gray-500">Type :</span>
                  <span class="tw-font-medium">{{ getRequestTypeLabel(wizardData()['requestType']) }}</span>
                </div>
              </div>

              <div class="tw-bg-gray-50 tw-rounded-xl tw-p-4">
                <p class="tw-text-xs tw-text-gray-500 tw-uppercase tw-font-semibold tw-mb-3">Protocole</p>
                <div class="tw-grid tw-grid-cols-2 tw-gap-2 tw-text-sm">
                  <span class="tw-text-gray-500">Titre :</span>
                  <span class="tw-font-medium">{{ protocolForm.value.title || '—' }}</span>
                  <span class="tw-text-gray-500">Phase :</span>
                  <span class="tw-font-medium">{{ protocolForm.value.studyPhase || '—' }}</span>
                  <span class="tw-text-gray-500">Patients prévus :</span>
                  <span class="tw-font-medium">{{ protocolForm.value.expectedPatients || '—' }}</span>
                </div>
              </div>

              <div class="tw-bg-gray-50 tw-rounded-xl tw-p-4">
                <p class="tw-text-xs tw-text-gray-500 tw-uppercase tw-font-semibold tw-mb-3">Équipe</p>
                <div class="tw-grid tw-grid-cols-2 tw-gap-2 tw-text-sm">
                  <span class="tw-text-gray-500">Investigateur principal :</span>
                  <span class="tw-font-medium">{{ teamForm.value.principalInvestigator || '—' }}</span>
                  <span class="tw-text-gray-500">Institution :</span>
                  <span class="tw-font-medium">{{ teamForm.value.institution || '—' }}</span>
                </div>
              </div>

              <div class="tw-bg-gray-50 tw-rounded-xl tw-p-4">
                <p class="tw-text-xs tw-text-gray-500 tw-uppercase tw-font-semibold tw-mb-3">Documents joints</p>
                @if (selectedFiles().length === 0) {
                  <p class="tw-text-sm tw-text-gray-400">Aucun document joint</p>
                } @else {
                  <ul class="tw-text-sm tw-space-y-1">
                    @for (file of selectedFiles(); track file.name) {
                      <li class="tw-text-gray-700">{{ file.name }} ({{ formatFileSize(file.size) }})</li>
                    }
                  </ul>
                }
              </div>

              <div class="tw-bg-blue-50 tw-border tw-border-blue-200 tw-rounded-xl tw-p-4">
                <p class="tw-text-sm tw-text-blue-700">
                  <i class="pi pi-info-circle tw-mr-2"></i>
                  En cliquant sur "Soumettre la demande", vous confirmez que les informations
                  fournies sont exactes et vous engagez à respecter les conditions du portail.
                </p>
              </div>
            </div>
          }

        </div>

        <!-- Navigation -->
        <div class="tw-flex tw-justify-between tw-mt-6">
          <p-button
            label="Précédent"
            icon="pi pi-chevron-left"
            severity="secondary"
            [disabled]="activeStep() === 0"
            (onClick)="prevStep()"
          />
          @if (activeStep() < steps.length - 1) {
            <p-button
              label="Suivant"
              icon="pi pi-chevron-right"
              iconPos="right"
              (onClick)="nextStep()"
            />
          } @else {
            <p-button
              label="Soumettre la demande"
              icon="pi pi-check"
              iconPos="right"
              [loading]="isSubmitting()"
              (onClick)="submitRequest()"
            />
          }
        </div>

      </div>
    </div>
  `,
})
export class RequestWizardComponent {

  private readonly exchangeService = inject(ExchangeService);
  private readonly router = inject(Router);
  private readonly fb = inject(FormBuilder);

  /** Étapes du wizard. */
  protected readonly steps = [
    { label: 'Type' },
    { label: 'Protocole' },
    { label: 'Équipe' },
    { label: 'Documents' },
    { label: 'Déclarations' },
    { label: 'Validation' },
  ];

  /** Index de l'étape courante. */
  protected readonly activeStep = signal(0);

  /** Données agrégées du wizard (étapes 1 et partielles). */
  protected readonly wizardData = signal<Record<string, unknown>>({});

  /** Fichiers sélectionnés pour l'upload. */
  protected readonly selectedFiles = signal<File[]>([]);

  /** Indique si la soumission finale est en cours. */
  protected readonly isSubmitting = signal(false);

  /** Messages d'erreur. */
  protected readonly messages = signal<Message[]>([]);

  protected readonly targetModuleOptions = TARGET_MODULE_OPTIONS;
  protected readonly requestTypeOptions = REQUEST_TYPE_OPTIONS;
  protected readonly studyPhaseOptions = STUDY_PHASE_OPTIONS;

  protected readonly protocolForm = this.fb.group({
    title:            ['', Validators.required],
    description:      [''],
    studyPhase:       [''],
    expectedPatients: [null as number | null],
  });

  protected readonly teamForm = this.fb.group({
    principalInvestigator: ['', Validators.required],
    institution:           ['', Validators.required],
    coInvestigators:       [''],
  });

  protected readonly declarationsForm = this.fb.group({
    rgpdCompliance:          [false],
    institutionAuthorization:[false],
    noConflictOfInterest:    [false],
  });

  /**
   * Définit une valeur dans wizardData.
   *
   * @param key Clé du champ
   * @param value Valeur à stocker
   */
  protected setWizardField(key: string, value: unknown): void {
    this.wizardData.update(data => ({ ...data, [key]: value }));
  }

  /**
   * Avance à l'étape suivante après validation basique.
   */
  protected nextStep(): void {
    const step = this.activeStep();
    if (step === 0) {
      if (!this.wizardData()['targetModule'] || !this.wizardData()['requestType']) {
        this.messages.set([{ severity: 'warn', summary: 'Sélection requise', detail: 'Veuillez sélectionner le module cible et le type de demande.' }]);
        return;
      }
    }
    if (step === 1 && this.protocolForm.invalid) {
      this.protocolForm.markAllAsTouched();
      this.messages.set([{ severity: 'warn', summary: 'Champs requis', detail: 'Veuillez renseigner le titre de l\'étude.' }]);
      return;
    }
    if (step === 2 && this.teamForm.invalid) {
      this.teamForm.markAllAsTouched();
      this.messages.set([{ severity: 'warn', summary: 'Champs requis', detail: 'Investigateur principal et institution sont obligatoires.' }]);
      return;
    }
    this.messages.set([]);
    this.activeStep.update(s => Math.min(s + 1, this.steps.length - 1));
  }

  /** Revient à l'étape précédente. */
  protected prevStep(): void {
    this.messages.set([]);
    this.activeStep.update(s => Math.max(s - 1, 0));
  }

  /**
   * Traite la sélection de fichiers.
   *
   * @param event Événement de sélection de fichier HTML
   */
  protected onFilesSelected(event: Event): void {
    const input = event.target as HTMLInputElement;
    if (input.files) {
      this.selectedFiles.set(Array.from(input.files));
    }
  }

  /**
   * Formate une taille en octets pour l'affichage.
   *
   * @param bytes Taille en octets
   * @returns Chaîne formatée (ex: "1.2 Mo")
   */
  protected formatFileSize(bytes: number): string {
    if (bytes < 1024) return `${bytes} o`;
    if (bytes < 1024 * 1024) return `${(bytes / 1024).toFixed(1)} Ko`;
    return `${(bytes / (1024 * 1024)).toFixed(1)} Mo`;
  }

  /**
   * Retourne le label lisible d'un TargetModule.
   *
   * @param value Valeur de l'enum
   */
  protected getModuleLabel(value: unknown): string {
    return TARGET_MODULE_OPTIONS.find(o => o.value === value)?.label ?? String(value ?? '—');
  }

  /**
   * Retourne le label lisible d'un ExchangeRequestType.
   *
   * @param value Valeur de l'enum
   */
  protected getRequestTypeLabel(value: unknown): string {
    return REQUEST_TYPE_OPTIONS.find(o => o.value === value)?.label ?? String(value ?? '—');
  }

  /**
   * Soumet la demande finale.
   * Crée la demande puis appelle submitRequest pour la passer en SUBMITTED.
   */
  protected submitRequest(): void {
    this.isSubmitting.set(true);
    this.messages.set([]);

    const rawPayload = {
      ...this.wizardData(),
      ...this.protocolForm.value,
      ...this.teamForm.value,
    };
    // Supprimer les valeurs null pour satisfaire Partial<ExchangeRequest>
    const payload = Object.fromEntries(
      Object.entries(rawPayload).filter(([, v]) => v !== null)
    );

    this.exchangeService.createRequest(payload as Partial<import('../../../core/models/exchange.model').ExchangeRequest>).subscribe({
      next: (req) => {
        this.exchangeService.submitRequest(req.id).subscribe({
          next: () => {
            this.isSubmitting.set(false);
            this.router.navigate(['/exchange/tracking']);
          },
          error: () => {
            this.isSubmitting.set(false);
            this.router.navigate(['/exchange/tracking']);
          },
        });
      },
      error: (err) => {
        this.isSubmitting.set(false);
        this.messages.set([{
          severity: 'error',
          summary: 'Erreur',
          detail: err.error?.detail ?? 'Erreur lors de la soumission. Veuillez réessayer.',
        }]);
      },
    });
  }
}
