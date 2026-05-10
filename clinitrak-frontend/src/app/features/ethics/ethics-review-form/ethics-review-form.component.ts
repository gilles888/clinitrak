import { Component, computed, inject, OnInit, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ReactiveFormsModule, FormBuilder, FormGroup, Validators } from '@angular/forms';
import { ActivatedRoute, Router } from '@angular/router';
import { ButtonModule } from 'primeng/button';
import { CalendarModule } from 'primeng/calendar';
import { CardModule } from 'primeng/card';
import { DropdownModule } from 'primeng/dropdown';
import { InputTextModule } from 'primeng/inputtext';
import { StepperModule } from 'primeng/stepper';
import { TextareaModule } from 'primeng/textarea';
import { ToastModule } from 'primeng/toast';
import { MessageService } from 'primeng/api';
import { EthicsService } from '../../../core/services/ethics.service';
import {
  REVIEW_DECISION_OPTIONS,
  REVIEW_TYPE_OPTIONS,
  DecisionUpdateRequest,
  EthicsReviewCreateRequest,
  EthicsReviewResponse,
} from '../../../core/models/ethics.model';

/**
 * Composant formulaire de création / mise à jour d'un avis CE.
 *
 * <p>Fonctionne en deux modes :
 * - Mode <strong>CRÉATION</strong> (route {@code /ethics/reviews/new}) : formulaire en 2 étapes
 *   via {@link StepperModule} pour saisir l'étude, le type et les détails.
 * - Mode <strong>DÉCISION</strong> (route {@code /ethics/reviews/:id/decision}) : formulaire
 *   simplifié pour enregistrer la décision du comité sur un avis existant.
 *
 * <p>La détection du mode est faite via un {@link computed} basé sur l'URL courante.
 * La soumission appelle respectivement {@link EthicsService#createReview} ou
 * {@link EthicsService#updateDecision}.
 */
@Component({
  selector: 'app-ethics-review-form',
  standalone: true,
  imports: [
    CommonModule,
    ReactiveFormsModule,
    ButtonModule,
    CalendarModule,
    CardModule,
    DropdownModule,
    InputTextModule,
    StepperModule,
    TextareaModule,
    ToastModule,
  ],
  providers: [MessageService],
  template: `
    <p-toast />

    <div class="tw-max-w-3xl tw-mx-auto tw-space-y-4">

      <!-- En-tête -->
      <div class="tw-flex tw-items-center tw-gap-3">
        <p-button
          icon="pi pi-arrow-left"
          severity="secondary"
          [text]="true"
          [rounded]="true"
          (onClick)="router.navigate(['/ethics/reviews'])"
          pTooltip="Retour à la liste"
        />
        <div>
          <h2 class="tw-text-2xl tw-font-bold tw-text-gray-900">
            @if (isDecisionMode()) {
              Décision CE — {{ existingReview()?.ethicsNumber }}
            } @else {
              Nouvel avis CE
            }
          </h2>
          <p class="tw-text-sm tw-text-gray-500 tw-mt-0.5">
            @if (isDecisionMode()) {
              Enregistrez la décision du comité d'éthique
            } @else {
              Soumettez une nouvelle demande au comité d'éthique
            }
          </p>
        </div>
      </div>

      <!-- ───── MODE DÉCISION ───── -->
      @if (isDecisionMode()) {
        <div class="tw-bg-white tw-rounded-xl tw-border tw-border-gray-200 tw-shadow-sm">
          <div class="tw-px-6 tw-py-4 tw-border-b tw-border-gray-100">
            <h3 class="tw-font-semibold tw-text-gray-800">Enregistrement de la décision</h3>
          </div>

          @if (isLoadingReview()) {
            <div class="tw-flex tw-justify-center tw-py-10">
              <i class="pi pi-spin pi-spinner tw-text-3xl tw-text-blue-500"></i>
            </div>
          }

          @if (!isLoadingReview() && decisionForm) {
            <form [formGroup]="decisionForm" class="tw-p-6 tw-space-y-5">

              <!-- Décision -->
              <div class="tw-flex tw-flex-col tw-gap-1">
                <label class="tw-text-sm tw-font-medium tw-text-gray-700">
                  Décision <span class="tw-text-red-500">*</span>
                </label>
                <p-dropdown
                  formControlName="decision"
                  [options]="decisionOptions"
                  optionLabel="label"
                  optionValue="value"
                  placeholder="Sélectionner la décision"
                  styleClass="tw-w-full"
                />
                @if (decisionForm.get('decision')?.invalid && decisionForm.get('decision')?.touched) {
                  <span class="tw-text-xs tw-text-red-500">Ce champ est obligatoire.</span>
                }
              </div>

              <!-- Date de décision -->
              <div class="tw-flex tw-flex-col tw-gap-1">
                <label class="tw-text-sm tw-font-medium tw-text-gray-700">
                  Date de décision <span class="tw-text-red-500">*</span>
                </label>
                <p-calendar
                  formControlName="decisionDate"
                  dateFormat="dd/mm/yy"
                  [showIcon]="true"
                  styleClass="tw-w-full"
                />
                @if (decisionForm.get('decisionDate')?.invalid && decisionForm.get('decisionDate')?.touched) {
                  <span class="tw-text-xs tw-text-red-500">Ce champ est obligatoire.</span>
                }
              </div>

              <!-- Date de revue -->
              <div class="tw-flex tw-flex-col tw-gap-1">
                <label class="tw-text-sm tw-font-medium tw-text-gray-700">Date de revue du comité</label>
                <p-calendar
                  formControlName="reviewDate"
                  dateFormat="dd/mm/yy"
                  [showIcon]="true"
                  styleClass="tw-w-full"
                />
              </div>

              <!-- Prochaine revue -->
              <div class="tw-flex tw-flex-col tw-gap-1">
                <label class="tw-text-sm tw-font-medium tw-text-gray-700">Prochaine revue requise</label>
                <p-calendar
                  formControlName="nextReviewDate"
                  dateFormat="dd/mm/yy"
                  [showIcon]="true"
                  styleClass="tw-w-full"
                />
              </div>

              <!-- Commentaires -->
              <div class="tw-flex tw-flex-col tw-gap-1">
                <label class="tw-text-sm tw-font-medium tw-text-gray-700">Commentaires / conditions</label>
                <textarea
                  pTextarea
                  formControlName="comments"
                  rows="4"
                  placeholder="Motivations, conditions d'approbation, informations complémentaires requises..."
                  class="tw-w-full"
                ></textarea>
              </div>

              <div class="tw-flex tw-justify-end tw-gap-2 tw-pt-2">
                <p-button
                  label="Annuler"
                  severity="secondary"
                  (onClick)="router.navigate(['/ethics/reviews'])"
                />
                <p-button
                  label="Enregistrer la décision"
                  icon="pi pi-check"
                  [loading]="isSubmitting()"
                  [disabled]="decisionForm.invalid"
                  (onClick)="onDecisionSubmit()"
                />
              </div>

            </form>
          }
        </div>
      }

      <!-- ───── MODE CRÉATION (Stepper 2 étapes) ───── -->
      @if (!isDecisionMode()) {
        <p-stepper [activeStep]="activeStep()">

          <!-- Étape 1 : Identification de l'étude -->
          <p-step-panel [value]="0" header="Identification de l'étude">
            <ng-template pTemplate="content">
              <div class="tw-bg-white tw-rounded-xl tw-border tw-border-gray-200 tw-p-6 tw-space-y-5">

                <form [formGroup]="createForm" class="tw-space-y-5">

                  <!-- Study ID -->
                  <div class="tw-flex tw-flex-col tw-gap-1">
                    <label class="tw-text-sm tw-font-medium tw-text-gray-700">
                      Identifiant de l'étude (UUID) <span class="tw-text-red-500">*</span>
                    </label>
                    <input
                      pInputText
                      formControlName="studyId"
                      placeholder="xxxxxxxx-xxxx-xxxx-xxxx-xxxxxxxxxxxx"
                      class="tw-w-full tw-font-mono"
                    />
                    @if (createForm.get('studyId')?.invalid && createForm.get('studyId')?.touched) {
                      <span class="tw-text-xs tw-text-red-500">L'identifiant de l'étude est obligatoire.</span>
                    }
                    <p class="tw-text-xs tw-text-gray-400 tw-mt-0.5">
                      Entrez l'UUID de l'étude clinique telle qu'elle apparaît dans le Study Service.
                    </p>
                  </div>

                  <!-- Type d'avis -->
                  <div class="tw-flex tw-flex-col tw-gap-1">
                    <label class="tw-text-sm tw-font-medium tw-text-gray-700">
                      Type d'avis <span class="tw-text-red-500">*</span>
                    </label>
                    <p-dropdown
                      formControlName="reviewType"
                      [options]="reviewTypeOptions"
                      optionLabel="label"
                      optionValue="value"
                      placeholder="Sélectionner le type"
                      styleClass="tw-w-full"
                    />
                    @if (createForm.get('reviewType')?.invalid && createForm.get('reviewType')?.touched) {
                      <span class="tw-text-xs tw-text-red-500">Ce champ est obligatoire.</span>
                    }
                  </div>

                  <!-- Rapporteur -->
                  <div class="tw-flex tw-flex-col tw-gap-1">
                    <label class="tw-text-sm tw-font-medium tw-text-gray-700">Rapporteur</label>
                    <input
                      pInputText
                      formControlName="rapporteurName"
                      placeholder="Nom du rapporteur désigné"
                      class="tw-w-full"
                    />
                  </div>

                </form>

                <div class="tw-flex tw-justify-end tw-pt-2">
                  <p-button
                    label="Suivant"
                    icon="pi pi-arrow-right"
                    iconPos="right"
                    [disabled]="createForm.get('studyId')?.invalid || createForm.get('reviewType')?.invalid"
                    (onClick)="goToStep(1)"
                  />
                </div>
              </div>
            </ng-template>
          </p-step-panel>

          <!-- Étape 2 : Détails et commentaires -->
          <p-step-panel [value]="1" header="Détails et commentaires">
            <ng-template pTemplate="content">
              <div class="tw-bg-white tw-rounded-xl tw-border tw-border-gray-200 tw-p-6 tw-space-y-5">

                <form [formGroup]="createForm" class="tw-space-y-5">

                  <!-- Date de soumission -->
                  <div class="tw-flex tw-flex-col tw-gap-1">
                    <label class="tw-text-sm tw-font-medium tw-text-gray-700">
                      Date de soumission <span class="tw-text-red-500">*</span>
                    </label>
                    <p-calendar
                      formControlName="submissionDate"
                      dateFormat="dd/mm/yy"
                      [showIcon]="true"
                      [maxDate]="today"
                      styleClass="tw-w-full"
                    />
                    @if (createForm.get('submissionDate')?.invalid && createForm.get('submissionDate')?.touched) {
                      <span class="tw-text-xs tw-text-red-500">Ce champ est obligatoire.</span>
                    }
                  </div>

                  <!-- Commentaires -->
                  <div class="tw-flex tw-flex-col tw-gap-1">
                    <label class="tw-text-sm tw-font-medium tw-text-gray-700">Commentaires</label>
                    <textarea
                      pTextarea
                      formControlName="comments"
                      rows="4"
                      placeholder="Informations complémentaires sur la soumission..."
                      class="tw-w-full"
                    ></textarea>
                  </div>

                  <!-- Récapitulatif -->
                  <div class="tw-bg-blue-50 tw-border tw-border-blue-200 tw-rounded-lg tw-p-4 tw-space-y-2">
                    <p class="tw-text-sm tw-font-semibold tw-text-blue-800">Récapitulatif</p>
                    <div class="tw-grid tw-grid-cols-2 tw-gap-2 tw-text-sm">
                      <div class="tw-text-gray-600">ID Étude :</div>
                      <div class="tw-font-mono tw-text-gray-800 tw-text-xs">{{ createForm.get('studyId')?.value || '—' }}</div>
                      <div class="tw-text-gray-600">Type :</div>
                      <div class="tw-font-semibold tw-text-gray-800">{{ getReviewTypeLabel(createForm.get('reviewType')?.value) }}</div>
                      <div class="tw-text-gray-600">Rapporteur :</div>
                      <div class="tw-text-gray-800">{{ createForm.get('rapporteurName')?.value || '—' }}</div>
                    </div>
                  </div>

                </form>

                <div class="tw-flex tw-justify-between tw-pt-2">
                  <p-button
                    label="Précédent"
                    icon="pi pi-arrow-left"
                    severity="secondary"
                    (onClick)="goToStep(0)"
                  />
                  <p-button
                    label="Soumettre l'avis"
                    icon="pi pi-check"
                    [loading]="isSubmitting()"
                    [disabled]="createForm.invalid"
                    (onClick)="onCreateSubmit()"
                  />
                </div>
              </div>
            </ng-template>
          </p-step-panel>

        </p-stepper>
      }

    </div>
  `,
})
export class EthicsReviewFormComponent implements OnInit {

  protected readonly router = inject(Router);
  private readonly route = inject(ActivatedRoute);
  private readonly ethicsService = inject(EthicsService);
  private readonly fb = inject(FormBuilder);
  private readonly messageService = inject(MessageService);

  /** Options des dropdowns. */
  protected readonly reviewTypeOptions = REVIEW_TYPE_OPTIONS;
  protected readonly decisionOptions = REVIEW_DECISION_OPTIONS;

  /** Date du jour pour limiter le calendrier de soumission. */
  protected readonly today = new Date();

  // ─── État réactif ──────────────────────────────────────────
  /** Vrai si la route contient /decision (mode mise à jour de décision). */
  protected readonly isDecisionMode = computed(() =>
    this.route.snapshot.url.map(s => s.path).join('/').includes('decision'),
  );

  /** Étape active du stepper (0 ou 1). */
  protected readonly activeStep = signal(0);
  /** Indicateur de soumission en cours. */
  protected readonly isSubmitting = signal(false);
  /** Indicateur de chargement de l'avis existant. */
  protected readonly isLoadingReview = signal(false);
  /** Avis existant chargé en mode décision. */
  protected readonly existingReview = signal<EthicsReviewResponse | null>(null);

  /** Formulaire réactif pour le mode création. */
  protected createForm!: FormGroup;
  /** Formulaire réactif pour le mode décision. */
  protected decisionForm!: FormGroup;

  // ─────────────────────────────────────────────────────────────

  ngOnInit(): void {
    if (this.isDecisionMode()) {
      this.initDecisionForm();
      const id = this.route.snapshot.paramMap.get('id');
      if (id) {
        this.loadExistingReview(id);
      }
    } else {
      this.initCreateForm();
    }
  }

  /** Initialise le formulaire de création en 2 étapes. */
  private initCreateForm(): void {
    this.createForm = this.fb.group({
      studyId:        ['', Validators.required],
      reviewType:     [null, Validators.required],
      rapporteurName: [''],
      submissionDate: [null, Validators.required],
      comments:       [''],
    });
  }

  /** Initialise le formulaire de mise à jour de décision. */
  private initDecisionForm(): void {
    this.decisionForm = this.fb.group({
      decision:      [null, Validators.required],
      decisionDate:  [null, Validators.required],
      reviewDate:    [null],
      nextReviewDate:[null],
      comments:      [''],
    });
  }

  /**
   * Charge un avis CE existant pour préremplir les métadonnées affichées.
   *
   * @param id identifiant UUID de l'avis CE
   */
  private loadExistingReview(id: string): void {
    this.isLoadingReview.set(true);
    this.ethicsService.getReview(id).subscribe({
      next: (review) => {
        this.existingReview.set(review);
        this.isLoadingReview.set(false);
      },
      error: (err) => {
        console.error('Erreur chargement avis CE', err);
        this.isLoadingReview.set(false);
      },
    });
  }

  /**
   * Navigue vers l'étape indiquée dans le stepper.
   *
   * @param step numéro de l'étape cible (0 ou 1)
   */
  protected goToStep(step: number): void {
    this.activeStep.set(step);
  }

  /**
   * Soumet le formulaire de création d'un nouvel avis CE.
   * Navigue vers {@code /ethics/reviews} en cas de succès.
   */
  protected onCreateSubmit(): void {
    if (this.createForm.invalid) {
      this.createForm.markAllAsTouched();
      return;
    }

    const raw = this.createForm.value;
    const request: EthicsReviewCreateRequest = {
      studyId:        raw.studyId,
      reviewType:     raw.reviewType,
      submissionDate: this.formatDate(raw.submissionDate),
      rapporteurName: raw.rapporteurName || undefined,
      comments:       raw.comments || undefined,
    };

    this.isSubmitting.set(true);
    this.ethicsService.createReview(request).subscribe({
      next: () => {
        this.messageService.add({ severity: 'success', summary: 'Succès', detail: 'Avis CE soumis avec succès.' });
        this.isSubmitting.set(false);
        setTimeout(() => this.router.navigate(['/ethics/reviews']), 1500);
      },
      error: (err) => {
        console.error('Erreur création avis CE', err);
        this.messageService.add({ severity: 'error', summary: 'Erreur', detail: 'Impossible de soumettre l\'avis CE.' });
        this.isSubmitting.set(false);
      },
    });
  }

  /**
   * Soumet la mise à jour de la décision CE sur l'avis existant.
   * Navigue vers {@code /ethics/reviews} en cas de succès.
   */
  protected onDecisionSubmit(): void {
    if (this.decisionForm.invalid) {
      this.decisionForm.markAllAsTouched();
      return;
    }

    const id = this.route.snapshot.paramMap.get('id');
    if (!id) return;

    const raw = this.decisionForm.value;
    const request: DecisionUpdateRequest = {
      decision:      raw.decision,
      decisionDate:  this.formatDate(raw.decisionDate),
      reviewDate:    raw.reviewDate     ? this.formatDate(raw.reviewDate)     : undefined,
      nextReviewDate:raw.nextReviewDate  ? this.formatDate(raw.nextReviewDate) : undefined,
      comments:      raw.comments || undefined,
    };

    this.isSubmitting.set(true);
    this.ethicsService.updateDecision(id, request).subscribe({
      next: () => {
        this.messageService.add({ severity: 'success', summary: 'Succès', detail: 'Décision enregistrée.' });
        this.isSubmitting.set(false);
        setTimeout(() => this.router.navigate(['/ethics/reviews']), 1500);
      },
      error: (err) => {
        console.error('Erreur mise à jour décision', err);
        this.messageService.add({ severity: 'error', summary: 'Erreur', detail: 'Impossible d\'enregistrer la décision.' });
        this.isSubmitting.set(false);
      },
    });
  }

  /**
   * Retourne le libellé d'un type d'avis CE depuis sa valeur enum.
   *
   * @param value valeur de l'enum ReviewType
   * @returns libellé affiché
   */
  protected getReviewTypeLabel(value: string | null): string {
    if (!value) return '—';
    return REVIEW_TYPE_OPTIONS.find(o => o.value === value)?.label ?? value;
  }

  /**
   * Formate un objet Date (issu de p-calendar) en chaîne ISO 8601 (yyyy-MM-dd).
   *
   * @param date valeur issue du calendrier PrimeNG
   * @returns chaîne ISO 8601
   */
  private formatDate(date: Date | string): string {
    if (typeof date === 'string') return date;
    const d = date as Date;
    const y = d.getFullYear();
    const m = String(d.getMonth() + 1).padStart(2, '0');
    const day = String(d.getDate()).padStart(2, '0');
    return `${y}-${m}-${day}`;
  }
}
