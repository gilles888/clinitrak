import {
  Component,
  effect,
  inject,
  OnInit,
  signal,
} from '@angular/core';
import { CommonModule, DatePipe } from '@angular/common';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { RouterLink } from '@angular/router';
import { debounceTime, distinctUntilChanged, Subject } from 'rxjs';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { ButtonModule } from 'primeng/button';
import { CalendarModule } from 'primeng/calendar';
import { CardModule } from 'primeng/card';
import { DropdownModule } from 'primeng/dropdown';
import { InputNumberModule } from 'primeng/inputnumber';
import { InputTextModule } from 'primeng/inputtext';
import { TableModule } from 'primeng/table';
import { TextareaModule } from 'primeng/textarea';
import { PharmacyService } from '../../../core/services/pharmacy.service';
import { Dispensation, InvestigationalDrug } from '../../../core/models/pharmacy.model';

/**
 * Composant formulaire de dispensation de médicament à un patient.
 *
 * <p>Permet d'enregistrer un acte de dispensation avec sélection du médicament
 * (liste dynamique), du patient, du pharmacien et du prescripteur.
 *
 * <p>L'historique des dispensations du patient est automatiquement rechargé
 * 500 ms après chaque saisie du code patient (via debounce sur patientCode$ Subject).
 */
@Component({
  selector: 'app-dispensation-form',
  standalone: true,
  imports: [
    CommonModule,
    DatePipe,
    RouterLink,
    ReactiveFormsModule,
    ButtonModule,
    CalendarModule,
    CardModule,
    DropdownModule,
    InputNumberModule,
    InputTextModule,
    TableModule,
    TextareaModule,
  ],
  template: `
    <div class="tw-space-y-6">

      <!-- En-tête -->
      <div class="tw-flex tw-items-center tw-justify-between">
        <div>
          <h2 class="tw-text-2xl tw-font-bold tw-text-gray-900">Nouvelle dispensation</h2>
          <p class="tw-text-sm tw-text-gray-500 tw-mt-1">Enregistrement d'un acte de dispensation</p>
        </div>
        <a
          routerLink="/pharmacy"
          class="p-button p-button-secondary tw-inline-flex tw-items-center tw-gap-2"
        >
          <i class="pi pi-arrow-left"></i>
          <span>Retour</span>
        </a>
      </div>

      <!-- Formulaire -->
      <p-card styleClass="tw-border tw-border-gray-100">
        <form [formGroup]="form" (ngSubmit)="onSubmit()" class="tw-space-y-4">

          <div class="tw-grid tw-grid-cols-1 md:tw-grid-cols-2 tw-gap-4">

            <!-- Code patient -->
            <div>
              <label class="tw-block tw-text-sm tw-font-medium tw-mb-1">
                Code patient <span class="tw-text-red-500">*</span>
              </label>
              <input
                pInputText
                formControlName="patientCode"
                placeholder="Ex : PAT-001"
                class="tw-w-full"
              />
            </div>

            <!-- ID Étude -->
            <div>
              <label class="tw-block tw-text-sm tw-font-medium tw-mb-1">
                ID Étude <span class="tw-text-red-500">*</span>
              </label>
              <input
                pInputText
                formControlName="studyId"
                placeholder="UUID de l'étude"
                class="tw-w-full"
              />
            </div>

            <!-- Médicament -->
            <div>
              <label class="tw-block tw-text-sm tw-font-medium tw-mb-1">
                Médicament <span class="tw-text-red-500">*</span>
              </label>
              <p-dropdown
                formControlName="drugId"
                [options]="drugOptions()"
                optionLabel="label"
                optionValue="value"
                placeholder="Sélectionner un médicament"
                [filter]="true"
                filterPlaceholder="Rechercher…"
                class="tw-w-full"
              />
            </div>

            <!-- Numéro de visite -->
            <div>
              <label class="tw-block tw-text-sm tw-font-medium tw-mb-1">Numéro de visite</label>
              <input
                pInputText
                formControlName="visitNumber"
                placeholder="Ex : V3"
                class="tw-w-full"
              />
            </div>

            <!-- Date de dispensation -->
            <div>
              <label class="tw-block tw-text-sm tw-font-medium tw-mb-1">
                Date de dispensation <span class="tw-text-red-500">*</span>
              </label>
              <p-calendar
                formControlName="dispensationDate"
                dateFormat="dd/mm/yy"
                [showIcon]="true"
                [maxDate]="today"
                class="tw-w-full"
              />
            </div>

            <!-- Quantité -->
            <div>
              <label class="tw-block tw-text-sm tw-font-medium tw-mb-1">
                Quantité <span class="tw-text-red-500">*</span>
              </label>
              <p-inputNumber
                formControlName="quantity"
                [min]="1"
                [showButtons]="true"
                class="tw-w-full"
              />
            </div>

            <!-- ID Pharmacien -->
            <div>
              <label class="tw-block tw-text-sm tw-font-medium tw-mb-1">
                ID Pharmacien <span class="tw-text-red-500">*</span>
              </label>
              <input
                pInputText
                formControlName="pharmacistId"
                placeholder="UUID du pharmacien"
                class="tw-w-full"
              />
            </div>

            <!-- ID Prescripteur -->
            <div>
              <label class="tw-block tw-text-sm tw-font-medium tw-mb-1">
                ID Prescripteur <span class="tw-text-red-500">*</span>
              </label>
              <input
                pInputText
                formControlName="prescriberId"
                placeholder="UUID du prescripteur"
                class="tw-w-full"
              />
            </div>

          </div>

          <!-- Prescription (référence) -->
          <div>
            <label class="tw-block tw-text-sm tw-font-medium tw-mb-1">Référence ordonnance</label>
            <input
              pInputText
              formControlName="prescription"
              placeholder="N° ordonnance ou référence"
              class="tw-w-full"
            />
          </div>

          <!-- Notes -->
          <div>
            <label class="tw-block tw-text-sm tw-font-medium tw-mb-1">Notes</label>
            <textarea
              pTextarea
              formControlName="notes"
              rows="3"
              class="tw-w-full"
              placeholder="Observations, instructions particulières…"
            ></textarea>
          </div>

          <!-- Bouton de soumission -->
          <div class="tw-flex tw-justify-end tw-pt-2">
            <button
              pButton
              type="submit"
              label="Enregistrer la dispensation"
              icon="pi pi-check"
              [disabled]="form.invalid || isSubmitting()"
              [loading]="isSubmitting()"
            ></button>
          </div>

          <!-- Message de succès -->
          @if (submitSuccess()) {
            <div class="tw-p-3 tw-bg-green-50 tw-border tw-border-green-200 tw-rounded tw-flex tw-items-center tw-gap-2">
              <i class="pi pi-check-circle tw-text-green-600"></i>
              <span class="tw-text-sm tw-text-green-700">Dispensation enregistrée avec succès.</span>
            </div>
          }

        </form>
      </p-card>

      <!-- Historique patient -->
      @if (patientHistory().length > 0) {
        <p-card styleClass="tw-border tw-border-blue-100">
          <ng-template pTemplate="header">
            <div class="tw-px-4 tw-pt-4">
              <h3 class="tw-font-semibold tw-text-gray-800">
                Dernières dispensations — {{ form.value.patientCode }}
              </h3>
              <p class="tw-text-xs tw-text-gray-500">5 dernières</p>
            </div>
          </ng-template>
          <p-table
            [value]="patientHistory()"
            styleClass="tw-text-sm"
          >
            <ng-template pTemplate="header">
              <tr class="tw-bg-gray-50">
                <th class="tw-text-xs tw-font-semibold tw-text-gray-600">Médicament</th>
                <th class="tw-text-xs tw-font-semibold tw-text-gray-600">Date</th>
                <th class="tw-text-xs tw-font-semibold tw-text-gray-600 tw-text-right">Qté</th>
                <th class="tw-text-xs tw-font-semibold tw-text-gray-600">Visite</th>
                <th class="tw-text-xs tw-font-semibold tw-text-gray-600">Notes</th>
              </tr>
            </ng-template>
            <ng-template pTemplate="body" let-d>
              <tr class="hover:tw-bg-gray-50">
                <td class="tw-px-3 tw-py-2 tw-font-medium">{{ d.drugName }}</td>
                <td class="tw-px-3 tw-py-2 tw-text-xs">{{ d.dispensationDate | date:'dd/MM/yyyy' }}</td>
                <td class="tw-px-3 tw-py-2 tw-text-right">{{ d.quantity }}</td>
                <td class="tw-px-3 tw-py-2 tw-text-xs tw-text-gray-500">{{ d.visitNumber ?? '—' }}</td>
                <td class="tw-px-3 tw-py-2 tw-text-xs tw-text-gray-500">{{ d.notes ?? '—' }}</td>
              </tr>
            </ng-template>
          </p-table>
        </p-card>
      }

    </div>
  `,
})
export class DispensationFormComponent implements OnInit {

  private readonly pharmacyService = inject(PharmacyService);
  private readonly fb = inject(FormBuilder);

  /** Formulaire de dispensation. */
  protected readonly form = this.fb.group({
    patientCode:      ['', Validators.required],
    studyId:          ['', Validators.required],
    drugId:           ['', Validators.required],
    visitNumber:      [''],
    dispensationDate: [null as Date | null, Validators.required],
    quantity:         [1, [Validators.required, Validators.min(1)]],
    pharmacistId:     ['', Validators.required],
    prescriberId:     ['', Validators.required],
    prescription:     [''],
    notes:            [''],
  });

  /** Liste des médicaments disponibles. */
  protected readonly drugs = signal<InvestigationalDrug[]>([]);

  /** Options dropdown calculées depuis la liste des médicaments. */
  protected readonly drugOptions = signal<{ label: string; value: string }[]>([]);

  /** Historique des 5 dernières dispensations du patient. */
  protected readonly patientHistory = signal<Dispensation[]>([]);

  /** Indicateur de soumission en cours. */
  protected readonly isSubmitting = signal(false);

  /** Indicateur de succès après soumission. */
  protected readonly submitSuccess = signal(false);

  /** Date d'aujourd'hui pour la borne max du calendrier. */
  protected readonly today = new Date();

  /** Subject pour le debounce de la recherche d'historique patient. */
  private readonly patientCode$ = new Subject<string>();

  // ─────────────────────────────────────────────────────────────

  constructor() {
    // Debounce sur le code patient pour charger l'historique
    this.patientCode$
      .pipe(debounceTime(500), distinctUntilChanged(), takeUntilDestroyed())
      .subscribe(code => {
        if (code.trim().length > 2) {
          this.loadPatientHistory(code.trim());
        } else {
          this.patientHistory.set([]);
        }
      });

    // Écoute le signal patientCode via effect
    effect(() => {
      const code = this.form.get('patientCode')?.value ?? '';
      this.patientCode$.next(code);
    });
  }

  ngOnInit(): void {
    this.loadDrugs();
  }

  /**
   * Charge la liste des médicaments disponibles pour le dropdown.
   */
  private loadDrugs(): void {
    this.pharmacyService.getDrugs().subscribe({
      next: drugs => {
        this.drugs.set(drugs);
        this.drugOptions.set(
          drugs.map(d => ({ label: `${d.drugName} (${d.formLabel})`, value: d.id })),
        );
      },
      error: err => console.error('Erreur chargement médicaments', err),
    });
  }

  /**
   * Charge les 5 dernières dispensations d'un patient donné.
   *
   * @param patientCode code anonymisé du patient
   */
  private loadPatientHistory(patientCode: string): void {
    this.pharmacyService.getDispensationsByPatient(patientCode).subscribe({
      next: list => this.patientHistory.set(list.slice(0, 5)),
      error: () => this.patientHistory.set([]),
    });
  }

  /**
   * Soumet le formulaire de dispensation à l'API.
   */
  protected onSubmit(): void {
    if (this.form.invalid) return;
    this.isSubmitting.set(true);
    this.submitSuccess.set(false);

    const value = this.form.value;
    const payload: Record<string, unknown> = {
      patientCode:      value.patientCode,
      studyId:          value.studyId,
      drugId:           value.drugId,
      visitNumber:      value.visitNumber ?? null,
      dispensationDate: (value.dispensationDate as Date | null)?.toISOString().slice(0, 10) ?? null,
      quantity:         value.quantity,
      pharmacistId:     value.pharmacistId,
      prescriberId:     value.prescriberId,
      prescription:     value.prescription ?? null,
      notes:            value.notes ?? null,
    };

    this.pharmacyService.createDispensation(payload).subscribe({
      next: () => {
        this.isSubmitting.set(false);
        this.submitSuccess.set(true);
        this.form.reset({ quantity: 1 });
        this.patientHistory.set([]);
      },
      error: err => {
        console.error('Erreur création dispensation', err);
        this.isSubmitting.set(false);
      },
    });
  }
}
