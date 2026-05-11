import { Component, inject, OnInit, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { RouterLink } from '@angular/router';
import { ButtonModule } from 'primeng/button';
import { CalendarModule } from 'primeng/calendar';
import { CardModule } from 'primeng/card';
import { DropdownModule } from 'primeng/dropdown';
import { InputNumberModule } from 'primeng/inputnumber';
import { InputTextModule } from 'primeng/inputtext';
import { PharmacyService } from '../../../core/services/pharmacy.service';
import { InvestigationalDrug, StockImportResult } from '../../../core/models/pharmacy.model';

/**
 * Composant de réception de stock de médicaments.
 *
 * <p>Propose deux modes de saisie :
 * <ul>
 *   <li>Saisie manuelle via un formulaire ReactiveForm</li>
 *   <li>Import en masse via un fichier CSV ou XLSX</li>
 * </ul>
 *
 * <p>Les médicaments disponibles sont chargés au init pour alimenter
 * le dropdown de sélection.
 */
@Component({
  selector: 'app-stock-receipt',
  standalone: true,
  imports: [
    CommonModule,
    RouterLink,
    ReactiveFormsModule,
    ButtonModule,
    CalendarModule,
    CardModule,
    DropdownModule,
    InputNumberModule,
    InputTextModule,
  ],
  template: `
    <div class="tw-space-y-6">

      <!-- En-tête -->
      <div class="tw-flex tw-items-center tw-justify-between">
        <div>
          <h2 class="tw-text-2xl tw-font-bold tw-text-gray-900">Réception de stock</h2>
          <p class="tw-text-sm tw-text-gray-500 tw-mt-1">Enregistrement d'un lot de médicaments reçu</p>
        </div>
        <a
          routerLink="/pharmacy/stocks"
          class="p-button p-button-secondary tw-inline-flex tw-items-center tw-gap-2"
        >
          <i class="pi pi-arrow-left"></i>
          <span>Voir les stocks</span>
        </a>
      </div>

      <!-- Section 1 — Saisie manuelle -->
      <p-card header="Saisie manuelle" styleClass="tw-border tw-border-gray-100">
        <form [formGroup]="form" (ngSubmit)="onSubmit()" class="tw-space-y-4">

          <div class="tw-grid tw-grid-cols-1 md:tw-grid-cols-2 tw-gap-4">

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

            <!-- Numéro de lot -->
            <div>
              <label class="tw-block tw-text-sm tw-font-medium tw-mb-1">
                Numéro de lot <span class="tw-text-red-500">*</span>
              </label>
              <input
                pInputText
                formControlName="batchNumber"
                placeholder="Ex : LOT-2026-001"
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

            <!-- Unité -->
            <div>
              <label class="tw-block tw-text-sm tw-font-medium tw-mb-1">
                Unité <span class="tw-text-red-500">*</span>
              </label>
              <input
                pInputText
                formControlName="unit"
                placeholder="Ex : comprimés, mL, flacons"
                class="tw-w-full"
              />
            </div>

            <!-- Date de réception -->
            <div>
              <label class="tw-block tw-text-sm tw-font-medium tw-mb-1">
                Date de réception <span class="tw-text-red-500">*</span>
              </label>
              <p-calendar
                formControlName="receivedDate"
                dateFormat="dd/mm/yy"
                [showIcon]="true"
                [maxDate]="today"
                class="tw-w-full"
              />
            </div>

            <!-- Date de péremption -->
            <div>
              <label class="tw-block tw-text-sm tw-font-medium tw-mb-1">
                Date de péremption <span class="tw-text-red-500">*</span>
              </label>
              <p-calendar
                formControlName="expiryDate"
                dateFormat="dd/mm/yy"
                [showIcon]="true"
                class="tw-w-full"
              />
            </div>

            <!-- Emplacement -->
            <div class="md:tw-col-span-2">
              <label class="tw-block tw-text-sm tw-font-medium tw-mb-1">Emplacement</label>
              <input
                pInputText
                formControlName="location"
                placeholder="Ex : Armoire A, Réfrigérateur 2"
                class="tw-w-full"
              />
            </div>

          </div>

          <!-- Bouton -->
          <div class="tw-flex tw-justify-end tw-pt-2">
            <button
              pButton
              type="submit"
              label="Enregistrer la réception"
              icon="pi pi-check"
              [disabled]="form.invalid || isSubmitting()"
              [loading]="isSubmitting()"
            ></button>
          </div>

          <!-- Succès -->
          @if (submitSuccess()) {
            <div class="tw-p-3 tw-bg-green-50 tw-border tw-border-green-200 tw-rounded tw-flex tw-items-center tw-gap-2">
              <i class="pi pi-check-circle tw-text-green-600"></i>
              <span class="tw-text-sm tw-text-green-700">Stock enregistré avec succès.</span>
            </div>
          }

        </form>
      </p-card>

      <!-- Section 2 — Import fichier -->
      <p-card header="Import CSV / XLSX" styleClass="tw-border tw-border-gray-100">
        <div class="tw-mt-2 tw-p-4 tw-border tw-border-dashed tw-border-gray-300 tw-rounded-lg">
          <p class="tw-text-xs tw-text-gray-500 tw-mb-3">
            Format attendu : <code class="tw-font-mono tw-bg-gray-100 tw-px-1 tw-rounded">
              drugId, quantity, unit, receivedDate (YYYY-MM-DD), expiryDate (YYYY-MM-DD), batchNumber, location
            </code>
          </p>
          <input
            type="file"
            accept=".csv,.xlsx"
            (change)="onFileSelected($event)"
            class="tw-block tw-text-sm"
          />
          <button
            pButton
            label="Importer"
            icon="pi pi-upload"
            [disabled]="!selectedFile() || isImporting()"
            [loading]="isImporting()"
            (click)="importFile()"
            class="tw-mt-3"
          ></button>

          @if (importResult()) {
            <div class="tw-mt-3 tw-p-3 tw-bg-gray-50 tw-rounded tw-border tw-border-gray-200">
              <p class="tw-text-sm tw-text-green-600 tw-font-medium">
                <i class="pi pi-check-circle tw-mr-1"></i>
                {{ importResult()!.imported }} ligne(s) importée(s)
              </p>
              @if (importResult()!.failed > 0) {
                <p class="tw-text-sm tw-text-red-600 tw-mt-1 tw-font-medium">
                  <i class="pi pi-times-circle tw-mr-1"></i>
                  {{ importResult()!.failed }} échec(s)
                </p>
                @for (err of importResult()!.errors; track err) {
                  <p class="tw-text-xs tw-text-red-500 tw-mt-0.5 tw-pl-4">{{ err }}</p>
                }
              }
            </div>
          }
        </div>
      </p-card>

    </div>
  `,
})
export class StockReceiptComponent implements OnInit {

  private readonly pharmacyService = inject(PharmacyService);
  private readonly fb = inject(FormBuilder);

  /** Formulaire de réception manuelle. */
  protected readonly form = this.fb.group({
    drugId:       ['', Validators.required],
    batchNumber:  ['', Validators.required],
    quantity:     [1, [Validators.required, Validators.min(1)]],
    unit:         ['', Validators.required],
    receivedDate: [null as Date | null, Validators.required],
    expiryDate:   [null as Date | null, Validators.required],
    location:     [''],
  });

  /** Options dropdown des médicaments disponibles. */
  protected readonly drugOptions = signal<{ label: string; value: string }[]>([]);

  /** Fichier sélectionné pour l'import. */
  protected readonly selectedFile = signal<File | null>(null);

  /** Résultat du dernier import. */
  protected readonly importResult = signal<StockImportResult | null>(null);

  /** Indicateur d'import en cours. */
  protected readonly isImporting = signal(false);

  /** Indicateur de soumission manuelle en cours. */
  protected readonly isSubmitting = signal(false);

  /** Indicateur de succès après soumission manuelle. */
  protected readonly submitSuccess = signal(false);

  /** Date d'aujourd'hui pour la borne max du calendrier de réception. */
  protected readonly today = new Date();

  // ─────────────────────────────────────────────────────────────

  ngOnInit(): void {
    this.loadDrugs();
  }

  /**
   * Charge la liste des médicaments pour alimenter le dropdown.
   */
  private loadDrugs(): void {
    this.pharmacyService.getDrugs().subscribe({
      next: (drugs: InvestigationalDrug[]) => {
        this.drugOptions.set(
          drugs.map(d => ({ label: `${d.drugName} (${d.formLabel})`, value: d.id })),
        );
      },
      error: err => console.error('Erreur chargement médicaments', err),
    });
  }

  /**
   * Soumet le formulaire de réception manuelle à l'API.
   */
  protected onSubmit(): void {
    if (this.form.invalid) return;
    this.isSubmitting.set(true);
    this.submitSuccess.set(false);

    const value = this.form.value;
    const payload: Record<string, unknown> = {
      drugId:       value.drugId,
      batchNumber:  value.batchNumber,
      quantity:     value.quantity,
      unit:         value.unit,
      receivedDate: (value.receivedDate as Date | null)?.toISOString().slice(0, 10) ?? null,
      expiryDate:   (value.expiryDate as Date | null)?.toISOString().slice(0, 10) ?? null,
      location:     value.location ?? null,
    };

    this.pharmacyService.receiveStock(payload).subscribe({
      next: () => {
        this.isSubmitting.set(false);
        this.submitSuccess.set(true);
        this.form.reset({ quantity: 1 });
      },
      error: err => {
        console.error('Erreur réception stock', err);
        this.isSubmitting.set(false);
      },
    });
  }

  /**
   * Capture le fichier sélectionné par l'utilisateur.
   *
   * @param event événement change de l'input file natif
   */
  protected onFileSelected(event: Event): void {
    const input = event.target as HTMLInputElement;
    if (input.files?.length) {
      this.selectedFile.set(input.files[0]);
      this.importResult.set(null);
    }
  }

  /**
   * Démarre l'import du fichier CSV/XLSX sélectionné.
   */
  protected importFile(): void {
    const file = this.selectedFile();
    if (!file) return;
    this.isImporting.set(true);
    this.importResult.set(null);
    this.pharmacyService.importStocksFromCsv(file).subscribe({
      next: result => {
        this.importResult.set(result);
        this.isImporting.set(false);
      },
      error: err => {
        console.error('Erreur import CSV', err);
        this.isImporting.set(false);
      },
    });
  }
}
