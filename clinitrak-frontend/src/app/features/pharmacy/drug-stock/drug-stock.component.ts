import { Component, computed, inject, OnInit, signal } from '@angular/core';
import { CommonModule, DatePipe } from '@angular/common';
import { RouterLink } from '@angular/router';
import { FormsModule } from '@angular/forms';
import { ButtonModule } from 'primeng/button';
import { CardModule } from 'primeng/card';
import { DialogModule } from 'primeng/dialog';
import { DropdownModule } from 'primeng/dropdown';
import { InputTextModule } from 'primeng/inputtext';
import { TableModule } from 'primeng/table';
import { TagModule } from 'primeng/tag';
import { PharmacyService } from '../../../core/services/pharmacy.service';
import {
  DrugStock,
  STOCK_STATUS_OPTIONS,
  STOCK_STATUS_SEVERITY,
  StockStatus,
} from '../../../core/models/pharmacy.model';

/**
 * Composant liste et gestion des stocks de médicaments expérimentaux.
 *
 * <p>Affiche un tableau paginé des stocks avec filtrage par texte libre et par statut.
 * La coloration des lignes indique les niveaux d'urgence liés à la péremption :
 * rouge pour J-7, jaune pour J-30.
 *
 * <p>Un dialog permet de changer le statut d'une unité de stock sélectionnée
 * (ex : QUARANTINE → AVAILABLE).
 */
@Component({
  selector: 'app-drug-stock',
  standalone: true,
  imports: [
    CommonModule,
    DatePipe,
    RouterLink,
    FormsModule,
    ButtonModule,
    CardModule,
    DialogModule,
    DropdownModule,
    InputTextModule,
    TableModule,
    TagModule,
  ],
  template: `
    <div class="tw-space-y-4">

      <!-- En-tête -->
      <div class="tw-flex tw-items-center tw-justify-between">
        <div>
          <h2 class="tw-text-2xl tw-font-bold tw-text-gray-900">Stocks de médicaments</h2>
          <p class="tw-text-sm tw-text-gray-500 tw-mt-1">
            {{ filteredStocks().length }} unité(s) affichée(s)
          </p>
        </div>
        <a
          routerLink="/pharmacy/receive"
          class="p-button p-button-primary tw-inline-flex tw-items-center tw-gap-2"
        >
          <i class="pi pi-plus"></i>
          <span>Réceptionner un stock</span>
        </a>
      </div>

      <!-- Filtres -->
      <div class="tw-flex tw-flex-wrap tw-gap-3">
        <span class="p-input-icon-left">
          <i class="pi pi-search"></i>
          <input
            pInputText
            type="text"
            placeholder="Médicament, lot, emplacement…"
            [value]="filterText()"
            (input)="filterText.set($any($event.target).value)"
            class="tw-w-64"
          />
        </span>
        <p-dropdown
          [options]="statusFilterOptions"
          [ngModel]="filterStatus()"
          (ngModelChange)="filterStatus.set($event)"
          placeholder="Tous les statuts"
          [showClear]="true"
          optionLabel="label"
          optionValue="value"
          class="tw-min-w-40"
        />
      </div>

      <!-- Chargement -->
      @if (isLoading()) {
        <div class="tw-flex tw-justify-center tw-py-12">
          <i class="pi pi-spin pi-spinner tw-text-4xl tw-text-blue-500"></i>
        </div>
      } @else {
        <p-card styleClass="tw-border tw-border-gray-100 tw-p-0">
          <p-table
            [value]="filteredStocks()"
            [rows]="15"
            [paginator]="true"
            styleClass="tw-text-sm"
            [rowHover]="true"
          >
            <ng-template pTemplate="header">
              <tr class="tw-bg-gray-50">
                <th class="tw-text-xs tw-font-semibold tw-text-gray-600">Médicament</th>
                <th class="tw-text-xs tw-font-semibold tw-text-gray-600">Étude</th>
                <th class="tw-text-xs tw-font-semibold tw-text-gray-600">Lot</th>
                <th class="tw-text-xs tw-font-semibold tw-text-gray-600 tw-text-right">Quantité</th>
                <th class="tw-text-xs tw-font-semibold tw-text-gray-600">Unité</th>
                <th class="tw-text-xs tw-font-semibold tw-text-gray-600">Emplacement</th>
                <th class="tw-text-xs tw-font-semibold tw-text-gray-600">Péremption</th>
                <th class="tw-text-xs tw-font-semibold tw-text-gray-600">Statut</th>
                <th class="tw-text-xs tw-font-semibold tw-text-gray-600">Actions</th>
              </tr>
            </ng-template>
            <ng-template pTemplate="body" let-stock>
              <tr [class]="getRowClass(stock)">
                <td class="tw-px-3 tw-py-2 tw-font-medium tw-text-gray-800">{{ stock.drugName }}</td>
                <td class="tw-px-3 tw-py-2 tw-font-mono tw-text-xs tw-text-gray-500">
                  {{ stock.studyId | slice:0:8 }}…
                </td>
                <td class="tw-px-3 tw-py-2 tw-font-mono tw-text-xs">{{ stock.batchNumber }}</td>
                <td class="tw-px-3 tw-py-2 tw-text-right tw-font-semibold">{{ stock.quantity }}</td>
                <td class="tw-px-3 tw-py-2 tw-text-gray-600">{{ stock.unit }}</td>
                <td class="tw-px-3 tw-py-2 tw-text-gray-500 tw-text-xs">{{ stock.location ?? '—' }}</td>
                <td class="tw-px-3 tw-py-2 tw-text-xs">{{ stock.expiryDate | date:'dd/MM/yyyy' }}</td>
                <td class="tw-px-3 tw-py-2">
                  <p-tag
                    [value]="stock.statusLabel"
                    [severity]="getStatusSeverity(stock)"
                  />
                </td>
                <td class="tw-px-3 tw-py-2">
                  <button
                    pButton
                    icon="pi pi-pencil"
                    severity="secondary"
                    [text]="true"
                    [rounded]="true"
                    pTooltip="Changer le statut"
                    (click)="openStatusDialog(stock)"
                  ></button>
                </td>
              </tr>
            </ng-template>
            <ng-template pTemplate="emptymessage">
              <tr>
                <td colspan="9" class="tw-py-10 tw-text-center tw-text-gray-400">
                  <i class="pi pi-inbox tw-text-3xl tw-block tw-mb-2"></i>
                  <p>Aucun stock trouvé</p>
                </td>
              </tr>
            </ng-template>
          </p-table>
        </p-card>
      }

    </div>

    <!-- Dialog changement de statut -->
    <p-dialog
      [(visible)]="showStatusDialog"
      [modal]="true"
      [style]="{ width: '400px' }"
      header="Changer le statut du stock"
    >
      @if (selectedStock()) {
        <div class="tw-space-y-4">
          <div>
            <p class="tw-text-sm tw-text-gray-500">Médicament</p>
            <p class="tw-font-semibold">{{ selectedStock()!.drugName }}</p>
          </div>
          <div>
            <p class="tw-text-sm tw-text-gray-500">Lot</p>
            <p class="tw-font-mono tw-text-sm">{{ selectedStock()!.batchNumber }}</p>
          </div>
          <div>
            <label class="tw-block tw-text-sm tw-font-medium tw-mb-1">Nouveau statut</label>
            <p-dropdown
              [options]="statusOptions"
              [(ngModel)]="newStatus"
              optionLabel="label"
              optionValue="value"
              placeholder="Sélectionner un statut"
              class="tw-w-full"
            />
          </div>
          <div class="tw-flex tw-gap-2 tw-pt-2">
            <button
              pButton
              label="Annuler"
              severity="secondary"
              (click)="closeStatusDialog()"
              class="tw-flex-1"
            ></button>
            <button
              pButton
              label="Confirmer"
              [disabled]="!newStatus || isUpdating()"
              [loading]="isUpdating()"
              (click)="confirmStatusChange()"
              class="tw-flex-1"
            ></button>
          </div>
        </div>
      }
    </p-dialog>
  `,
})
export class DrugStockComponent implements OnInit {

  private readonly pharmacyService = inject(PharmacyService);

  /** Indicateur de chargement initial. */
  protected readonly isLoading = signal(false);

  /** Indicateur de mise à jour de statut en cours. */
  protected readonly isUpdating = signal(false);

  /** Liste brute de tous les stocks chargés depuis l'API. */
  protected readonly stocks = signal<DrugStock[]>([]);

  /** Filtre texte libre (médicament, lot, emplacement). */
  protected readonly filterText = signal('');

  /** Filtre par statut de stock. */
  protected readonly filterStatus = signal<StockStatus | null>(null);

  /** Stock sélectionné pour le changement de statut. */
  protected readonly selectedStock = signal<DrugStock | null>(null);

  /** Nouveau statut choisi dans le dialog. */
  protected newStatus: StockStatus | null = null;

  /** Visibilité du dialog de changement de statut. */
  protected showStatusDialog = false;

  /** Options dropdown de filtrage (avec option "Tous"). */
  protected readonly statusFilterOptions = [
    { label: 'Tous les statuts', value: null },
    ...STOCK_STATUS_OPTIONS,
  ];

  /** Options dropdown pour la liste des statuts disponibles. */
  protected readonly statusOptions = STOCK_STATUS_OPTIONS;

  /**
   * Liste filtrée calculée à partir des signaux filterText et filterStatus.
   */
  protected readonly filteredStocks = computed(() => {
    const text = this.filterText().toLowerCase().trim();
    const status = this.filterStatus();
    return this.stocks().filter(s => {
      const matchText =
        !text ||
        s.drugName.toLowerCase().includes(text) ||
        s.batchNumber.toLowerCase().includes(text) ||
        (s.location ?? '').toLowerCase().includes(text);
      const matchStatus = !status || s.status === status;
      return matchText && matchStatus;
    });
  });

  // ─────────────────────────────────────────────────────────────

  ngOnInit(): void {
    this.loadStocks();
  }

  /**
   * Charge la liste complète des stocks depuis l'API.
   */
  private loadStocks(): void {
    this.isLoading.set(true);
    this.pharmacyService.getStocks().subscribe({
      next: stocks => {
        this.stocks.set(stocks);
        this.isLoading.set(false);
      },
      error: err => {
        console.error('Erreur chargement stocks', err);
        this.isLoading.set(false);
      },
    });
  }

  /**
   * Retourne la sévérité PrimeNG Tag pour le statut d'un stock.
   *
   * @param stock unité de stock
   * @returns sévérité PrimeNG
   */
  protected getStatusSeverity(stock: DrugStock): string {
    return STOCK_STATUS_SEVERITY[stock.status] ?? 'info';
  }

  /**
   * Retourne la classe CSS de coloration d'une ligne selon la date de péremption.
   *
   * @param stock unité de stock
   * @returns classe Tailwind (rouge J-7, jaune J-30, vide sinon)
   */
  protected getRowClass(stock: DrugStock): string {
    if (!stock.expiryDate) return '';
    const today = new Date();
    const expiry = new Date(stock.expiryDate);
    const diffDays = Math.ceil(
      (expiry.getTime() - today.getTime()) / (1000 * 60 * 60 * 24),
    );
    if (diffDays <= 7) return 'tw-bg-red-50';
    if (diffDays <= 30) return 'tw-bg-yellow-50';
    return '';
  }

  /**
   * Ouvre le dialog de changement de statut pour un stock donné.
   *
   * @param stock unité de stock à modifier
   */
  protected openStatusDialog(stock: DrugStock): void {
    this.selectedStock.set(stock);
    this.newStatus = stock.status;
    this.showStatusDialog = true;
  }

  /**
   * Ferme le dialog de changement de statut sans effectuer de modification.
   */
  protected closeStatusDialog(): void {
    this.showStatusDialog = false;
    this.selectedStock.set(null);
    this.newStatus = null;
  }

  /**
   * Confirme et applique le changement de statut via l'API.
   */
  protected confirmStatusChange(): void {
    const stock = this.selectedStock();
    if (!stock || !this.newStatus) return;
    this.isUpdating.set(true);
    this.pharmacyService.updateStockStatus(stock.id, this.newStatus).subscribe({
      next: updated => {
        this.stocks.update(list =>
          list.map(s => (s.id === updated.id ? updated : s)),
        );
        this.isUpdating.set(false);
        this.closeStatusDialog();
      },
      error: err => {
        console.error('Erreur changement statut', err);
        this.isUpdating.set(false);
      },
    });
  }
}
