import { Component, inject, OnInit, signal, computed } from '@angular/core';
import { CommonModule } from '@angular/common';
import { AbstractControl, FormBuilder, FormControl, ReactiveFormsModule, Validators } from '@angular/forms';
import { ActivatedRoute, Router } from '@angular/router';
import { ButtonModule } from 'primeng/button';
import { CardModule } from 'primeng/card';
import { CheckboxModule } from 'primeng/checkbox';
import { CalendarModule } from 'primeng/calendar';
import { DropdownModule } from 'primeng/dropdown';
import { InputNumberModule } from 'primeng/inputnumber';
import { InputTextModule } from 'primeng/inputtext';
import { InputTextareaModule } from 'primeng/inputtextarea';
import { StepperModule } from 'primeng/stepper';
import { TagModule } from 'primeng/tag';
import { ToastModule } from 'primeng/toast';
import { MessageService } from 'primeng/api';
import { StudyService } from '../../../core/services/study.service';
import {
  STUDY_TYPE_OPTIONS,
  SPONSOR_TYPE_OPTIONS,
  STUDY_PHASE_OPTIONS,
  StudyCreateRequest,
  StudyType,
  SponsorType,
  StudyPhase,
  StudyResponse,
} from '../../../core/models/study.model';
import { StudyStatusBadgeComponent } from '../../../shared/components/study-status-badge/study-status-badge.component';

/**
 * Composant formulaire de création et modification d'une étude clinique.
 *
 * <p>Utilise un {@code p-stepper} à 3 étapes :
 * <ol>
 *   <li>Identification — numéros réglementaires, type, sponsor</li>
 *   <li>Détails scientifiques — investigateur, phase, dates, enrollment</li>
 *   <li>Confirmation — récapitulatif en lecture seule avant envoi</li>
 * </ol>
 *
 * <p>Le mode CREATE vs EDIT est détecté automatiquement depuis le paramètre {@code :id}
 * dans la route. En mode EDIT, les données existantes sont préchargées.
 *
 * <p>La validation réactive (Angular Reactive Forms) garantit que {@code title},
 * {@code studyType} et {@code sponsorType} sont obligatoires.
 */
@Component({
  selector: 'app-study-form',
  standalone: true,
  imports: [
    CommonModule,
    ReactiveFormsModule,
    ButtonModule,
    CardModule,
    CheckboxModule,
    CalendarModule,
    DropdownModule,
    InputNumberModule,
    InputTextModule,
    InputTextareaModule,
    StepperModule,
    TagModule,
    ToastModule,
    StudyStatusBadgeComponent,
  ],
  providers: [MessageService],
  template: `
    <p-toast />

    <div class="tw-max-w-4xl tw-mx-auto tw-space-y-4">

      <!-- En-tête -->
      <div class="tw-flex tw-items-center tw-gap-3">
        <p-button icon="pi pi-arrow-left" severity="secondary" [rounded]="true" [text]="true" (onClick)="goBack()" />
        <div>
          <h2 class="tw-text-2xl tw-font-bold tw-text-gray-900">
            {{ isEditMode() ? 'Modifier l\'étude' : 'Nouvelle étude clinique' }}
          </h2>
          @if (isEditMode() && studyData()) {
            <p class="tw-text-sm tw-text-gray-500">{{ studyData()!.studyNumber }} — {{ studyData()!.title }}</p>
          }
        </div>
      </div>

      @if (isLoadingStudy()) {
        <div class="tw-flex tw-justify-center tw-py-16">
          <i class="pi pi-spin pi-spinner tw-text-5xl tw-text-blue-500"></i>
        </div>
      } @else {
        <!-- Stepper -->
        <p-stepper [linear]="true" [(activeStep)]="activeStepIndex">

          <!-- ─── Étape 1 : Identification ─── -->
          <p-stepperPanel header="Identification">
            <ng-template pTemplate="content" let-prevCallback="prevCallback" let-nextCallback="nextCallback">
              <div class="tw-pt-4 tw-space-y-6">
                <p-card styleClass="tw-border tw-border-gray-100">
                  <h3 class="tw-text-lg tw-font-semibold tw-text-gray-800 tw-mb-4">Informations générales</h3>
                  <div class="tw-grid tw-grid-cols-1 sm:tw-grid-cols-2 tw-gap-4">

                    <!-- Titre -->
                    <div class="tw-col-span-2 tw-flex tw-flex-col tw-gap-1">
                      <label class="tw-text-sm tw-font-medium tw-text-gray-700">
                        Titre complet <span class="tw-text-red-500">*</span>
                      </label>
                      <input
                        pInputText
                        formControlName="title"
                        [formControl]="f['title']"
                        placeholder="Titre officiel de l'étude..."
                        [class.ng-dirty]="f['title'].dirty"
                      />
                      @if (f['title'].invalid && f['title'].dirty) {
                        <small class="tw-text-red-500">Le titre est obligatoire.</small>
                      }
                    </div>

                    <!-- Acronyme -->
                    <div class="tw-flex tw-flex-col tw-gap-1">
                      <label class="tw-text-sm tw-font-medium tw-text-gray-700">Acronyme</label>
                      <input pInputText [formControl]="f['acronym']" placeholder="Ex: TRIAL-2025" />
                    </div>

                    <!-- Type d'étude -->
                    <div class="tw-flex tw-flex-col tw-gap-1">
                      <label class="tw-text-sm tw-font-medium tw-text-gray-700">
                        Type d'étude <span class="tw-text-red-500">*</span>
                      </label>
                      <p-dropdown
                        [options]="studyTypeOptions"
                        [formControl]="f['studyType']"
                        optionLabel="label"
                        optionValue="value"
                        placeholder="Sélectionner..."
                        styleClass="tw-w-full"
                      />
                      @if (f['studyType'].invalid && f['studyType'].dirty) {
                        <small class="tw-text-red-500">Le type d'étude est obligatoire.</small>
                      }
                    </div>

                    <!-- Type sponsor -->
                    <div class="tw-flex tw-flex-col tw-gap-1">
                      <label class="tw-text-sm tw-font-medium tw-text-gray-700">
                        Type de sponsor <span class="tw-text-red-500">*</span>
                      </label>
                      <p-dropdown
                        [options]="sponsorTypeOptions"
                        [formControl]="f['sponsorType']"
                        optionLabel="label"
                        optionValue="value"
                        placeholder="Sélectionner..."
                        styleClass="tw-w-full"
                      />
                      @if (f['sponsorType'].invalid && f['sponsorType'].dirty) {
                        <small class="tw-text-red-500">Le type de sponsor est obligatoire.</small>
                      }
                    </div>

                    <!-- Sponsor CUSL -->
                    <div class="tw-flex tw-items-center tw-gap-2 tw-pt-4">
                      <p-checkbox [formControl]="f['isSponsorCusl']" [binary]="true" inputId="isSponsorCusl" />
                      <label for="isSponsorCusl" class="tw-text-sm tw-font-medium tw-text-gray-700 tw-cursor-pointer">
                        Sponsor = CUSL (Cliniques Universitaires Saint-Luc)
                      </label>
                    </div>

                  </div>
                </p-card>

                <p-card styleClass="tw-border tw-border-gray-100">
                  <h3 class="tw-text-lg tw-font-semibold tw-text-gray-800 tw-mb-4">Numéros réglementaires</h3>
                  <div class="tw-grid tw-grid-cols-1 sm:tw-grid-cols-3 tw-gap-4">
                    <div class="tw-flex tw-flex-col tw-gap-1">
                      <label class="tw-text-sm tw-font-medium tw-text-gray-700">N° Éthique</label>
                      <input pInputText [formControl]="f['ethicsNumber']" placeholder="Comité d'éthique..." />
                    </div>
                    <div class="tw-flex tw-flex-col tw-gap-1">
                      <label class="tw-text-sm tw-font-medium tw-text-gray-700">N° EudraCT</label>
                      <input pInputText [formControl]="f['eudractNumber']" placeholder="EudraCT..." />
                    </div>
                    <div class="tw-flex tw-flex-col tw-gap-1">
                      <label class="tw-text-sm tw-font-medium tw-text-gray-700">N° CTIS</label>
                      <input pInputText [formControl]="f['ctisNumber']" placeholder="CTIS..." />
                    </div>
                  </div>
                </p-card>

                <div class="tw-flex tw-justify-end">
                  <p-button
                    label="Suivant"
                    icon="pi pi-arrow-right"
                    iconPos="right"
                    [disabled]="isStep1Invalid()"
                    (onClick)="nextStep()"
                  />
                </div>
              </div>
            </ng-template>
          </p-stepperPanel>

          <!-- ─── Étape 2 : Détails scientifiques ─── -->
          <p-stepperPanel header="Détails scientifiques">
            <ng-template pTemplate="content" let-prevCallback="prevCallback" let-nextCallback="nextCallback">
              <div class="tw-pt-4 tw-space-y-6">
                <p-card styleClass="tw-border tw-border-gray-100">
                  <h3 class="tw-text-lg tw-font-semibold tw-text-gray-800 tw-mb-4">Équipe & Science</h3>
                  <div class="tw-grid tw-grid-cols-1 sm:tw-grid-cols-2 tw-gap-4">

                    <div class="tw-flex tw-flex-col tw-gap-1">
                      <label class="tw-text-sm tw-font-medium tw-text-gray-700">Sponsor</label>
                      <input pInputText [formControl]="f['sponsor']" placeholder="Nom du sponsor..." />
                    </div>

                    <div class="tw-flex tw-flex-col tw-gap-1">
                      <label class="tw-text-sm tw-font-medium tw-text-gray-700">Investigateur principal</label>
                      <input pInputText [formControl]="f['principalInvestigator']" placeholder="Dr. ..." />
                    </div>

                    <div class="tw-flex tw-flex-col tw-gap-1">
                      <label class="tw-text-sm tw-font-medium tw-text-gray-700">Aire thérapeutique</label>
                      <input pInputText [formControl]="f['therapeuticArea']" placeholder="Oncologie, Cardiologie..." />
                    </div>

                    <div class="tw-flex tw-flex-col tw-gap-1">
                      <label class="tw-text-sm tw-font-medium tw-text-gray-700">Phase</label>
                      <p-dropdown
                        [options]="studyPhaseOptions"
                        [formControl]="f['phase']"
                        optionLabel="label"
                        optionValue="value"
                        placeholder="Sélectionner une phase"
                        styleClass="tw-w-full"
                      />
                    </div>

                  </div>
                </p-card>

                <p-card styleClass="tw-border tw-border-gray-100">
                  <h3 class="tw-text-lg tw-font-semibold tw-text-gray-800 tw-mb-4">Dates & Enrollment</h3>
                  <div class="tw-grid tw-grid-cols-1 sm:tw-grid-cols-2 lg:tw-grid-cols-3 tw-gap-4">

                    <div class="tw-flex tw-flex-col tw-gap-1">
                      <label class="tw-text-sm tw-font-medium tw-text-gray-700">Date de début</label>
                      <p-calendar
                        [formControl]="f['startDate']"
                        dateFormat="dd/mm/yy"
                        placeholder="dd/mm/yyyy"
                        [showIcon]="true"
                        styleClass="tw-w-full"
                      />
                    </div>

                    <div class="tw-flex tw-flex-col tw-gap-1">
                      <label class="tw-text-sm tw-font-medium tw-text-gray-700">Date de fin prévue</label>
                      <p-calendar
                        [formControl]="f['endDate']"
                        dateFormat="dd/mm/yy"
                        placeholder="dd/mm/yyyy"
                        [showIcon]="true"
                        styleClass="tw-w-full"
                      />
                    </div>

                    <div class="tw-flex tw-flex-col tw-gap-1">
                      <label class="tw-text-sm tw-font-medium tw-text-gray-700">Objectif d'inclusion</label>
                      <p-inputNumber
                        [formControl]="f['targetEnrollment']"
                        [min]="0"
                        placeholder="Nombre de patients"
                        styleClass="tw-w-full"
                      />
                    </div>

                  </div>
                </p-card>

                <p-card styleClass="tw-border tw-border-gray-100">
                  <h3 class="tw-text-lg tw-font-semibold tw-text-gray-800 tw-mb-4">Description</h3>
                  <textarea
                    pInputTextarea
                    [formControl]="f['description']"
                    rows="4"
                    placeholder="Description de l'étude, objectifs, critères d'inclusion..."
                    class="tw-w-full"
                  ></textarea>
                </p-card>

                <div class="tw-flex tw-justify-between">
                  <p-button
                    label="Précédent"
                    icon="pi pi-arrow-left"
                    severity="secondary"
                    (onClick)="prevStep()"
                  />
                  <p-button
                    label="Suivant"
                    icon="pi pi-arrow-right"
                    iconPos="right"
                    (onClick)="nextStep()"
                  />
                </div>
              </div>
            </ng-template>
          </p-stepperPanel>

          <!-- ─── Étape 3 : Confirmation ─── -->
          <p-stepperPanel header="Confirmation">
            <ng-template pTemplate="content" let-prevCallback="prevCallback">
              <div class="tw-pt-4 tw-space-y-6">

                <div class="tw-bg-blue-50 tw-border tw-border-blue-200 tw-rounded-lg tw-p-4 tw-flex tw-gap-3">
                  <i class="pi pi-info-circle tw-text-blue-500 tw-mt-0.5"></i>
                  <p class="tw-text-sm tw-text-blue-700">
                    Vérifiez les informations avant de
                    {{ isEditMode() ? 'enregistrer les modifications' : 'créer l\'étude' }}.
                  </p>
                </div>

                <!-- Récapitulatif -->
                <div class="tw-grid tw-grid-cols-1 lg:tw-grid-cols-2 tw-gap-6">

                  <p-card header="Identification" styleClass="tw-border tw-border-gray-100">
                    <div class="tw-space-y-3">
                      <div>
                        <span class="tw-text-xs tw-text-gray-500 tw-uppercase tw-tracking-wide">Titre</span>
                        <p class="tw-text-sm tw-font-medium tw-mt-0.5">{{ studyForm.get('title')?.value || '—' }}</p>
                      </div>
                      <div>
                        <span class="tw-text-xs tw-text-gray-500 tw-uppercase tw-tracking-wide">Acronyme</span>
                        <p class="tw-text-sm tw-mt-0.5">{{ studyForm.get('acronym')?.value || '—' }}</p>
                      </div>
                      <div>
                        <span class="tw-text-xs tw-text-gray-500 tw-uppercase tw-tracking-wide">Type d'étude</span>
                        <p class="tw-text-sm tw-mt-0.5">{{ studyTypeLabel() }}</p>
                      </div>
                      <div>
                        <span class="tw-text-xs tw-text-gray-500 tw-uppercase tw-tracking-wide">Type de sponsor</span>
                        <p class="tw-text-sm tw-mt-0.5">{{ sponsorTypeLabel() }}</p>
                      </div>
                      <div>
                        <span class="tw-text-xs tw-text-gray-500 tw-uppercase tw-tracking-wide">Sponsor CUSL</span>
                        <p class="tw-text-sm tw-mt-0.5">
                          {{ studyForm.get('isSponsorCusl')?.value ? 'Oui' : 'Non' }}
                        </p>
                      </div>
                    </div>
                  </p-card>

                  <p-card header="Numéros réglementaires" styleClass="tw-border tw-border-gray-100">
                    <div class="tw-space-y-3">
                      <div>
                        <span class="tw-text-xs tw-text-gray-500 tw-uppercase tw-tracking-wide">N° Éthique</span>
                        <p class="tw-text-sm tw-font-mono tw-mt-0.5">{{ studyForm.get('ethicsNumber')?.value || '—' }}</p>
                      </div>
                      <div>
                        <span class="tw-text-xs tw-text-gray-500 tw-uppercase tw-tracking-wide">EudraCT</span>
                        <p class="tw-text-sm tw-font-mono tw-mt-0.5">{{ studyForm.get('eudractNumber')?.value || '—' }}</p>
                      </div>
                      <div>
                        <span class="tw-text-xs tw-text-gray-500 tw-uppercase tw-tracking-wide">CTIS</span>
                        <p class="tw-text-sm tw-font-mono tw-mt-0.5">{{ studyForm.get('ctisNumber')?.value || '—' }}</p>
                      </div>
                    </div>
                  </p-card>

                  <p-card header="Équipe & Science" styleClass="tw-border tw-border-gray-100">
                    <div class="tw-space-y-3">
                      <div>
                        <span class="tw-text-xs tw-text-gray-500 tw-uppercase tw-tracking-wide">Sponsor</span>
                        <p class="tw-text-sm tw-mt-0.5">{{ studyForm.get('sponsor')?.value || '—' }}</p>
                      </div>
                      <div>
                        <span class="tw-text-xs tw-text-gray-500 tw-uppercase tw-tracking-wide">Investigateur principal</span>
                        <p class="tw-text-sm tw-mt-0.5">{{ studyForm.get('principalInvestigator')?.value || '—' }}</p>
                      </div>
                      <div>
                        <span class="tw-text-xs tw-text-gray-500 tw-uppercase tw-tracking-wide">Aire thérapeutique</span>
                        <p class="tw-text-sm tw-mt-0.5">{{ studyForm.get('therapeuticArea')?.value || '—' }}</p>
                      </div>
                      <div>
                        <span class="tw-text-xs tw-text-gray-500 tw-uppercase tw-tracking-wide">Phase</span>
                        <p class="tw-text-sm tw-mt-0.5">{{ phaseLabel() }}</p>
                      </div>
                    </div>
                  </p-card>

                  <p-card header="Dates & Enrollment" styleClass="tw-border tw-border-gray-100">
                    <div class="tw-space-y-3">
                      <div>
                        <span class="tw-text-xs tw-text-gray-500 tw-uppercase tw-tracking-wide">Date de début</span>
                        <p class="tw-text-sm tw-mt-0.5">{{ formatDateValue(studyForm.get('startDate')?.value) }}</p>
                      </div>
                      <div>
                        <span class="tw-text-xs tw-text-gray-500 tw-uppercase tw-tracking-wide">Date de fin</span>
                        <p class="tw-text-sm tw-mt-0.5">{{ formatDateValue(studyForm.get('endDate')?.value) }}</p>
                      </div>
                      <div>
                        <span class="tw-text-xs tw-text-gray-500 tw-uppercase tw-tracking-wide">Objectif inclusion</span>
                        <p class="tw-text-sm tw-mt-0.5">
                          {{ studyForm.get('targetEnrollment')?.value ?? '—' }} patients
                        </p>
                      </div>
                    </div>
                  </p-card>

                  @if (studyForm.get('description')?.value) {
                    <p-card header="Description" styleClass="tw-border tw-border-gray-100 lg:tw-col-span-2">
                      <p class="tw-text-sm tw-text-gray-700 tw-whitespace-pre-wrap">
                        {{ studyForm.get('description')?.value }}
                      </p>
                    </p-card>
                  }
                </div>

                <div class="tw-flex tw-justify-between">
                  <p-button
                    label="Précédent"
                    icon="pi pi-arrow-left"
                    severity="secondary"
                    (onClick)="prevStep()"
                  />
                  <p-button
                    [label]="isEditMode() ? 'Enregistrer les modifications' : submitLabel"
                    [icon]="isEditMode() ? 'pi pi-save' : 'pi pi-plus'"
                    [loading]="isSubmitting()"
                    [disabled]="studyForm.invalid"
                    (onClick)="onSubmit()"
                  />
                </div>

              </div>
            </ng-template>
          </p-stepperPanel>

        </p-stepper>
      }
    </div>
  `,
})
export class StudyFormComponent implements OnInit {

  private readonly studyService   = inject(StudyService);
  private readonly route          = inject(ActivatedRoute);
  private readonly router         = inject(Router);
  private readonly fb             = inject(FormBuilder);
  private readonly messageService = inject(MessageService);

  // ─── Options de dropdowns ───────────────────────────────────
  protected readonly studyTypeOptions   = STUDY_TYPE_OPTIONS;
  protected readonly sponsorTypeOptions = SPONSOR_TYPE_OPTIONS;
  protected readonly studyPhaseOptions  = STUDY_PHASE_OPTIONS;

  /** Label du bouton de soumission (mode création). */
  protected readonly submitLabel = "Créer l'étude";

  // ─── État réactif (signals) ──────────────────────────────────
  /** true si la route contient un paramètre :id (mode EDIT). */
  protected readonly isEditMode     = signal(false);
  /** Données de l'étude préchargées en mode EDIT. */
  protected readonly studyData      = signal<StudyResponse | null>(null);
  /** true pendant le chargement des données en mode EDIT. */
  protected readonly isLoadingStudy = signal(false);
  /** true pendant la soumission du formulaire. */
  protected readonly isSubmitting   = signal(false);
  /** Index de l'étape active du stepper (0-based). */
  protected activeStepIndex         = 0;

  private studyId: string | null = null;

  // ─── Formulaire réactif ─────────────────────────────────────
  protected readonly studyForm = this.fb.group({
    // Étape 1 — Identification
    title:              ['', [Validators.required, Validators.minLength(3)]],
    acronym:            [''],
    studyType:          [null as StudyType | null, Validators.required],
    sponsorType:        [null as SponsorType | null, Validators.required],
    isSponsorCusl:      [false],
    ethicsNumber:       [''],
    eudractNumber:      [''],
    ctisNumber:         [''],
    // Étape 2 — Détails
    sponsor:            [''],
    principalInvestigator: [''],
    therapeuticArea:    [''],
    phase:              [null as StudyPhase | null],
    startDate:          [null as Date | null],
    endDate:            [null as Date | null],
    targetEnrollment:   [null as number | null],
    description:        [''],
  });

  /** Raccourci vers les contrôles du formulaire. */
  protected get f(): { [key: string]: FormControl } {
    return this.studyForm.controls as { [key: string]: FormControl };
  }

  // ─── Computed labels pour le récapitulatif ──────────────────

  /** Libellé du type d'étude sélectionné. */
  protected readonly studyTypeLabel = computed(() => {
    const v = this.studyForm.get('studyType')?.value;
    return STUDY_TYPE_OPTIONS.find(o => o.value === v)?.label ?? '—';
  });

  /** Libellé du type de sponsor sélectionné. */
  protected readonly sponsorTypeLabel = computed(() => {
    const v = this.studyForm.get('sponsorType')?.value;
    return SPONSOR_TYPE_OPTIONS.find(o => o.value === v)?.label ?? '—';
  });

  /** Libellé de la phase sélectionnée. */
  protected readonly phaseLabel = computed(() => {
    const v = this.studyForm.get('phase')?.value;
    return STUDY_PHASE_OPTIONS.find(o => o.value === v)?.label ?? '—';
  });

  // ─────────────────────────────────────────────────────────────

  ngOnInit(): void {
    const id = this.route.snapshot.paramMap.get('id');
    if (id) {
      this.isEditMode.set(true);
      this.studyId = id;
      this.loadStudyForEdit(id);
    }
  }

  /**
   * Charge les données de l'étude existante pour préremplir le formulaire.
   *
   * @param id UUID de l'étude à modifier
   */
  private loadStudyForEdit(id: string): void {
    this.isLoadingStudy.set(true);
    this.studyService.getStudy(id).subscribe({
      next: (study) => {
        this.studyData.set(study);
        this.studyForm.patchValue({
          title:                 study.title,
          acronym:               study.acronym ?? '',
          studyType:             study.studyType,
          sponsorType:           study.sponsorType,
          isSponsorCusl:         study.isSponsorCusl,
          ethicsNumber:          study.ethicsNumber ?? '',
          eudractNumber:         study.eudractNumber ?? '',
          ctisNumber:            study.ctisNumber ?? '',
          sponsor:               study.sponsor,
          principalInvestigator: study.principalInvestigator,
          therapeuticArea:       study.therapeuticArea ?? '',
          phase:                 study.phase,
          startDate:             study.startDate ? new Date(study.startDate) : null,
          endDate:               study.endDate   ? new Date(study.endDate)   : null,
          targetEnrollment:      study.targetEnrollment ?? null,
          description:           study.description ?? '',
        });
        this.isLoadingStudy.set(false);
      },
      error: (err) => {
        console.error('Erreur chargement étude', err);
        this.messageService.add({
          severity: 'error',
          summary: 'Erreur',
          detail: 'Impossible de charger les données de l\'étude.',
        });
        this.isLoadingStudy.set(false);
      },
    });
  }

  /**
   * Vérifie si l'étape 1 est invalide (champs obligatoires manquants).
   *
   * @returns true si title, studyType ou sponsorType est invalide
   */
  protected isStep1Invalid(): boolean {
    return this.f['title'].invalid || this.f['studyType'].invalid || this.f['sponsorType'].invalid;
  }

  /** Passe à l'étape suivante du stepper. */
  protected nextStep(): void {
    if (this.activeStepIndex < 2) {
      this.activeStepIndex++;
    }
  }

  /** Revient à l'étape précédente du stepper. */
  protected prevStep(): void {
    if (this.activeStepIndex > 0) {
      this.activeStepIndex--;
    }
  }

  /**
   * Formate une valeur Date ou string ISO pour l'affichage dans le récapitulatif.
   *
   * @param value Date object ou string ISO
   * @returns chaîne formatée dd/MM/yyyy ou '—'
   */
  protected formatDateValue(value: Date | string | null | undefined): string {
    if (!value) return '—';
    const d = value instanceof Date ? value : new Date(value);
    if (isNaN(d.getTime())) return '—';
    return d.toLocaleDateString('fr-BE', { day: '2-digit', month: '2-digit', year: 'numeric' });
  }

  /**
   * Formate une Date en string ISO yyyy-MM-dd pour l'API.
   *
   * @param date objet Date
   * @returns string ISO ou undefined
   */
  private toIsoDate(date: Date | null | undefined): string | undefined {
    if (!date) return undefined;
    const d = date instanceof Date ? date : new Date(date);
    return d.toISOString().split('T')[0];
  }

  /**
   * Soumet le formulaire — crée ou met à jour l'étude selon le mode.
   * Navigue vers la page de détail après succès.
   */
  protected onSubmit(): void {
    if (this.studyForm.invalid) {
      this.studyForm.markAllAsTouched();
      return;
    }

    const values = this.studyForm.getRawValue();
    const request: StudyCreateRequest = {
      title:                 values.title!,
      acronym:               values.acronym || undefined,
      studyType:             values.studyType!,
      sponsorType:           values.sponsorType!,
      sponsor:               values.sponsor || '',
      principalInvestigator: values.principalInvestigator || '',
      therapeuticArea:       values.therapeuticArea || undefined,
      phase:                 values.phase ?? StudyPhase.NA,
      startDate:             this.toIsoDate(values.startDate),
      endDate:               this.toIsoDate(values.endDate),
      targetEnrollment:      values.targetEnrollment ?? undefined,
      isSponsorCusl:         values.isSponsorCusl ?? false,
      description:           values.description || undefined,
      ethicsNumber:          values.ethicsNumber || undefined,
      eudractNumber:         values.eudractNumber || undefined,
      ctisNumber:            values.ctisNumber || undefined,
    };

    this.isSubmitting.set(true);

    if (this.isEditMode() && this.studyId) {
      this.studyService.updateStudy(this.studyId, request).subscribe({
        next: (study) => {
          this.messageService.add({
            severity: 'success',
            summary: 'Succès',
            detail: 'Étude mise à jour avec succès.',
          });
          this.isSubmitting.set(false);
          this.router.navigate(['/studies', study.id]);
        },
        error: (err) => {
          console.error('Erreur mise à jour étude', err);
          this.messageService.add({
            severity: 'error',
            summary: 'Erreur',
            detail: 'Impossible de mettre à jour l\'étude.',
          });
          this.isSubmitting.set(false);
        },
      });
    } else {
      this.studyService.createStudy(request).subscribe({
        next: (study) => {
          this.messageService.add({
            severity: 'success',
            summary: 'Étude créée',
            detail: `L'étude ${study.studyNumber} a été créée avec succès.`,
          });
          this.isSubmitting.set(false);
          this.router.navigate(['/studies', study.id]);
        },
        error: (err) => {
          console.error('Erreur création étude', err);
          this.messageService.add({
            severity: 'error',
            summary: 'Erreur',
            detail: 'Impossible de créer l\'étude.',
          });
          this.isSubmitting.set(false);
        },
      });
    }
  }

  /** Retourne à la liste des études ou au détail en mode EDIT. */
  protected goBack(): void {
    if (this.isEditMode() && this.studyId) {
      this.router.navigate(['/studies', this.studyId]);
    } else {
      this.router.navigate(['/studies']);
    }
  }
}
