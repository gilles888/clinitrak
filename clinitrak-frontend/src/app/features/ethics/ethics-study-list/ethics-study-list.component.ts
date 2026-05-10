import { Component, inject, OnInit, signal } from '@angular/core';
import { CommonModule, DatePipe } from '@angular/common';
import { FormsModule, ReactiveFormsModule, FormBuilder, FormGroup, Validators } from '@angular/forms';
import { Router, RouterLink } from '@angular/router';
import { ButtonModule } from 'primeng/button';
import { CalendarModule } from 'primeng/calendar';
import { DialogModule } from 'primeng/dialog';
import { DropdownModule } from 'primeng/dropdown';
import { InputTextModule } from 'primeng/inputtext';
import { TableModule } from 'primeng/table';
import { TagModule } from 'primeng/tag';
import { TextareaModule } from 'primeng/textarea';
import { ToastModule } from 'primeng/toast';
import { TooltipModule } from 'primeng/tooltip';
import { MessageService } from 'primeng/api';
import { EthicsService } from '../../../core/services/ethics.service';
import {
  DECISION_SEVERITY,
  REVIEW_DECISION_OPTIONS,
  DecisionUpdateRequest,
  EthicsReviewResponse,
  ReviewDecision,
} from '../../../core/models/ethics.model';

/**
 * Composant de liste des avis Comité d'Éthique.
 *
 * <p>Affiche un tableau paginé de tous les avis CE avec leurs statuts.
 * Permet de déclencher la mise à jour d'une décision via un dialog intégré.
 *
 * <p>Les données sont chargées via {@link EthicsService#getReviews}.
 * Les erreurs et confirmations sont notifiées via {@link MessageService} (PrimeNG Toast).
 */
@Component({
  selector: 'app-ethics-study-list',
  standalone: true,
  imports: [
    CommonModule,
    DatePipe,
    FormsModule,
    ReactiveFormsModule,
    RouterLink,
    ButtonModule,
    CalendarModule,
    DialogModule,
    DropdownModule,
    InputTextModule,
    TableModule,
    TagModule,
    TextareaModule,
    ToastModule,
    TooltipModule,
  ],
  providers: [MessageService],
  template: `
    <p-toast />

    <div class="tw-space-y-4">

      <!-- En-tête -->
      <div class="tw-flex tw-items-center tw-justify-between">
        <div>
          <h2 class="tw-text-2xl tw-font-bold tw-text-gray-900">Avis Comité d'Éthique</h2>
          <p class="tw-text-sm tw-text-gray-500 tw-mt-1">
            {{ totalRecords() }} avis trouvé(s)
          </p>
        </div>
        <p-button
          label="Nouvel avis"
          icon="pi pi-plus"
          routerLink="/ethics/reviews/new"
          severity="primary"
        />
      </div>

      <!-- Table des avis CE -->
      <p-table
        [value]="reviews()"
        [lazy]="true"
        [paginator]="true"
        [rows]="pageSize()"
        [totalRecords]="totalRecords()"
        [loading]="isLoading()"
        [rowsPerPageOptions]="[10, 20, 50]"
        (onLazyLoad)="onLazyLoad($event)"
        styleClass="tw-border tw-border-gray-200 tw-rounded-lg"
        [rowHover]="true"
        dataKey="id"
      >
        <ng-template pTemplate="header">
          <tr class="tw-bg-gray-50">
            <th class="tw-font-semibold tw-text-sm">N° CE</th>
            <th class="tw-font-semibold tw-text-sm">ID Étude</th>
            <th class="tw-font-semibold tw-text-sm">Type</th>
            <th class="tw-font-semibold tw-text-sm">Date soumission</th>
            <th class="tw-font-semibold tw-text-sm">Rapporteur</th>
            <th class="tw-font-semibold tw-text-sm">Décision</th>
            <th class="tw-font-semibold tw-text-sm tw-text-center">Actions</th>
          </tr>
        </ng-template>

        <ng-template pTemplate="body" let-review>
          <tr>
            <td>
              <span class="tw-font-mono tw-text-blue-600 tw-text-sm tw-font-semibold">
                {{ review.ethicsNumber }}
              </span>
            </td>
            <td>
              <span class="tw-font-mono tw-text-gray-600 tw-text-xs">
                {{ review.studyId | slice:0:8 }}...
              </span>
            </td>
            <td class="tw-text-sm">{{ review.reviewTypeLabel }}</td>
            <td class="tw-text-sm">
              {{ review.submissionDate | date:'dd/MM/yyyy' }}
            </td>
            <td class="tw-text-sm tw-text-gray-600">
              {{ review.rapporteurName ?? '—' }}
            </td>
            <td>
              <p-tag
                [value]="review.decisionLabel"
                [severity]="getDecisionSeverity(review.decision)"
              />
            </td>
            <td>
              <div class="tw-flex tw-gap-1 tw-justify-center">
                @if (review.decision === ReviewDecision.PENDING) {
                  <p-button
                    icon="pi pi-check-square"
                    label="Décision"
                    severity="warning"
                    size="small"
                    [text]="true"
                    pTooltip="Enregistrer la décision"
                    (onClick)="openDecisionDialog(review)"
                  />
                }
                <p-button
                  icon="pi pi-eye"
                  severity="secondary"
                  size="small"
                  [rounded]="true"
                  [text]="true"
                  pTooltip="Voir le détail"
                  [routerLink]="['/ethics/reviews', review.id, 'decision']"
                />
              </div>
            </td>
          </tr>
        </ng-template>

        <ng-template pTemplate="emptymessage">
          <tr>
            <td colspan="7" class="tw-text-center tw-py-12 tw-text-gray-500">
              <i class="pi pi-inbox tw-text-4xl tw-mb-4 tw-block tw-text-gray-300"></i>
              Aucun avis CE trouvé.
            </td>
          </tr>
        </ng-template>

        <ng-template pTemplate="loadingbody">
          <tr>
            <td colspan="7" class="tw-text-center tw-py-12">
              <i class="pi pi-spin pi-spinner tw-text-4xl tw-text-blue-500"></i>
            </td>
          </tr>
        </ng-template>
      </p-table>

    </div>

    <!-- Dialog mise à jour de la décision -->
    <p-dialog
      header="Enregistrer la décision CE"
      [(visible)]="showDecisionDialog"
      [modal]="true"
      [style]="{ width: '520px' }"
      [closable]="true"
      [draggable]="false"
    >
      @if (decisionForm) {
        <form [formGroup]="decisionForm" class="tw-space-y-4 tw-pt-2">

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
              placeholder="Sélectionner une décision"
              styleClass="tw-w-full"
            />
            @if (decisionForm.get('decision')?.invalid && decisionForm.get('decision')?.touched) {
              <span class="tw-text-xs tw-text-red-500">La décision est obligatoire.</span>
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
              placeholder="JJ/MM/AAAA"
              [showIcon]="true"
              styleClass="tw-w-full"
            />
            @if (decisionForm.get('decisionDate')?.invalid && decisionForm.get('decisionDate')?.touched) {
              <span class="tw-text-xs tw-text-red-500">La date de décision est obligatoire.</span>
            }
          </div>

          <!-- Date de revue -->
          <div class="tw-flex tw-flex-col tw-gap-1">
            <label class="tw-text-sm tw-font-medium tw-text-gray-700">Date de revue</label>
            <p-calendar
              formControlName="reviewDate"
              dateFormat="dd/mm/yy"
              placeholder="JJ/MM/AAAA"
              [showIcon]="true"
              styleClass="tw-w-full"
            />
          </div>

          <!-- Prochaine revue -->
          <div class="tw-flex tw-flex-col tw-gap-1">
            <label class="tw-text-sm tw-font-medium tw-text-gray-700">Prochaine revue</label>
            <p-calendar
              formControlName="nextReviewDate"
              dateFormat="dd/mm/yy"
              placeholder="JJ/MM/AAAA"
              [showIcon]="true"
              styleClass="tw-w-full"
            />
          </div>

          <!-- Commentaires -->
          <div class="tw-flex tw-flex-col tw-gap-1">
            <label class="tw-text-sm tw-font-medium tw-text-gray-700">Commentaires</label>
            <textarea
              pTextarea
              formControlName="comments"
              rows="3"
              placeholder="Observations, conditions d'approbation..."
              class="tw-w-full"
            ></textarea>
          </div>

        </form>
      }

      <ng-template pTemplate="footer">
        <div class="tw-flex tw-gap-2 tw-justify-end">
          <p-button
            label="Annuler"
            severity="secondary"
            (onClick)="closeDecisionDialog()"
          />
          <p-button
            label="Enregistrer"
            icon="pi pi-check"
            [loading]="isSaving()"
            [disabled]="decisionForm?.invalid"
            (onClick)="onDecisionSubmit()"
          />
        </div>
      </ng-template>
    </p-dialog>
  `,
})
export class EthicsStudyListComponent implements OnInit {

  private readonly ethicsService = inject(EthicsService);
  private readonly fb = inject(FormBuilder);
  private readonly messageService = inject(MessageService);
  private readonly router = inject(Router);

  /** Expose l'enum ReviewDecision au template. */
  protected readonly ReviewDecision = ReviewDecision;
  /** Options du dropdown Décision. */
  protected readonly decisionOptions = REVIEW_DECISION_OPTIONS;

  // ─── État réactif ──────────────────────────────────────────
  /** Liste des avis CE de la page courante. */
  protected readonly reviews = signal<EthicsReviewResponse[]>([]);
  /** Nombre total d'avis correspondant aux critères. */
  protected readonly totalRecords = signal(0);
  /** Indicateur de chargement en cours. */
  protected readonly isLoading = signal(false);
  /** Indicateur de sauvegarde en cours. */
  protected readonly isSaving = signal(false);
  /** Page courante (0-based). */
  private readonly currentPage = signal(0);
  /** Nombre d'éléments par page. */
  protected readonly pageSize = signal(20);

  /** Visibilité du dialog de décision. */
  protected showDecisionDialog = false;
  /** Avis CE sélectionné pour la mise à jour de décision. */
  protected selectedReview: EthicsReviewResponse | null = null;
  /** Formulaire réactif de la décision. */
  protected decisionForm: FormGroup | null = null;

  // ─────────────────────────────────────────────────────────────

  ngOnInit(): void {
    this.loadReviews();
  }

  /**
   * Charge la page d'avis CE depuis l'API.
   */
  protected loadReviews(): void {
    this.isLoading.set(true);
    this.ethicsService.getReviews(this.currentPage(), this.pageSize()).subscribe({
      next: (page) => {
        this.reviews.set(page.content);
        this.totalRecords.set(page.totalElements);
        this.isLoading.set(false);
      },
      error: (err) => {
        console.error('Erreur chargement avis CE', err);
        this.messageService.add({ severity: 'error', summary: 'Erreur', detail: 'Impossible de charger les avis CE.' });
        this.isLoading.set(false);
      },
    });
  }

  /**
   * Ouvre le dialog de saisie de décision pour l'avis sélectionné.
   *
   * @param review l'avis CE à mettre à jour
   */
  protected openDecisionDialog(review: EthicsReviewResponse): void {
    this.selectedReview = review;
    this.decisionForm = this.fb.group({
      decision:      [null, Validators.required],
      decisionDate:  [null, Validators.required],
      reviewDate:    [null],
      nextReviewDate:[null],
      comments:      [''],
    });
    this.showDecisionDialog = true;
  }

  /** Ferme le dialog de décision et réinitialise l'état. */
  protected closeDecisionDialog(): void {
    this.showDecisionDialog = false;
    this.selectedReview = null;
    this.decisionForm = null;
  }

  /**
   * Soumet la mise à jour de la décision CE.
   * Ferme le dialog et recharge la liste en cas de succès.
   */
  protected onDecisionSubmit(): void {
    if (!this.decisionForm || this.decisionForm.invalid || !this.selectedReview) {
      this.decisionForm?.markAllAsTouched();
      return;
    }

    const raw = this.decisionForm.value;
    const request: DecisionUpdateRequest = {
      decision:      raw.decision,
      decisionDate:  this.formatDate(raw.decisionDate),
      reviewDate:    raw.reviewDate    ? this.formatDate(raw.reviewDate)     : undefined,
      nextReviewDate:raw.nextReviewDate ? this.formatDate(raw.nextReviewDate): undefined,
      comments:      raw.comments || undefined,
    };

    this.isSaving.set(true);
    this.ethicsService.updateDecision(this.selectedReview.id, request).subscribe({
      next: () => {
        this.messageService.add({ severity: 'success', summary: 'Succès', detail: 'Décision enregistrée.' });
        this.isSaving.set(false);
        this.closeDecisionDialog();
        this.loadReviews();
      },
      error: (err) => {
        console.error('Erreur mise à jour décision', err);
        this.messageService.add({ severity: 'error', summary: 'Erreur', detail: 'Impossible d\'enregistrer la décision.' });
        this.isSaving.set(false);
      },
    });
  }

  /**
   * Gère les événements de chargement lazy de la table PrimeNG.
   *
   * @param event événement PrimeNG LazyLoadEvent
   */
  protected onLazyLoad(event: { first: number; rows: number }): void {
    const page = Math.floor((event.first ?? 0) / (event.rows ?? this.pageSize()));
    this.currentPage.set(page);
    this.pageSize.set(event.rows ?? 20);
    this.loadReviews();
  }

  /**
   * Retourne la severité PrimeNG Tag pour une décision CE donnée.
   *
   * @param decision clé de la décision
   * @returns severité PrimeNG
   */
  protected getDecisionSeverity(decision: string): string {
    return DECISION_SEVERITY[decision] ?? 'info';
  }

  /**
   * Formate un objet Date (issu d'un p-calendar) en chaîne ISO 8601 (yyyy-MM-dd).
   *
   * @param date date issue du calendrier PrimeNG
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
