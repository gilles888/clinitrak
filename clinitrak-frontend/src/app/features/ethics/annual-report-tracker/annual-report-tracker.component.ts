import { Component, inject, OnInit, signal } from '@angular/core';
import { CommonModule, DatePipe } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { ButtonModule } from 'primeng/button';
import { CalendarModule } from 'primeng/calendar';
import { DialogModule } from 'primeng/dialog';
import { InputTextModule } from 'primeng/inputtext';
import { MessageModule } from 'primeng/message';
import { TableModule, TableLazyLoadEvent } from 'primeng/table';
import { TagModule } from 'primeng/tag';
import { ToastModule } from 'primeng/toast';
import { TooltipModule } from 'primeng/tooltip';
import { MessageService } from 'primeng/api';
import { EthicsService } from '../../../core/services/ethics.service';
import {
  ANNUAL_REPORT_STATUS_SEVERITY,
  AnnualReportResponse,
  AnnualReportStatus,
} from '../../../core/models/ethics.model';

/**
 * Composant de suivi des rapports annuels des études cliniques.
 *
 * <p>Affiche la liste paginée et triable des rapports annuels avec :
 * - indicateurs colorés selon l'échéance ({@link getDueDateClass})
 * - statut via PrimeNG Tag ({@link ANNUAL_REPORT_STATUS_SEVERITY})
 * - indicateurs de rappels envoyés (J-60, J-30, J-0)
 * - bouton "Marquer reçu" pour les rapports en attente ou en retard
 *
 * <p>Les données sont chargées via {@link EthicsService#getAnnualReports}.
 * La mise à jour de réception utilise {@link EthicsService#markReportReceived}.
 */
@Component({
  selector: 'app-annual-report-tracker',
  standalone: true,
  imports: [
    CommonModule,
    DatePipe,
    FormsModule,
    ButtonModule,
    CalendarModule,
    DialogModule,
    InputTextModule,
    MessageModule,
    TableModule,
    TagModule,
    ToastModule,
    TooltipModule,
  ],
  providers: [MessageService],
  template: `
    <p-toast />

    <div class="tw-space-y-5">

      <!-- En-tête -->
      <div class="tw-flex tw-items-center tw-justify-between">
        <div>
          <h2 class="tw-text-2xl tw-font-bold tw-text-gray-900">Rapports annuels</h2>
          <p class="tw-text-sm tw-text-gray-500 tw-mt-1">
            {{ totalRecords() }} rapport(s) au total
          </p>
        </div>
        <p-button
          icon="pi pi-refresh"
          severity="secondary"
          pTooltip="Recharger"
          (onClick)="loadReports()"
        />
      </div>

      <!-- KPIs / Alertes -->
      <div class="tw-grid tw-grid-cols-1 sm:tw-grid-cols-2 tw-gap-4">

        @if (overdueCount() > 0) {
          <div class="tw-bg-red-50 tw-border tw-border-red-300 tw-rounded-lg tw-p-4 tw-flex tw-items-center tw-gap-3">
            <i class="pi pi-exclamation-triangle tw-text-red-500 tw-text-2xl"></i>
            <div>
              <p class="tw-font-semibold tw-text-red-700">{{ overdueCount() }} rapport(s) en retard</p>
              <p class="tw-text-sm tw-text-red-600">Des rappels doivent être envoyés immédiatement.</p>
            </div>
          </div>
        }

        @if (dueSoonCount() > 0) {
          <div class="tw-bg-orange-50 tw-border tw-border-orange-200 tw-rounded-lg tw-p-4 tw-flex tw-items-center tw-gap-3">
            <i class="pi pi-clock tw-text-orange-500 tw-text-2xl"></i>
            <div>
              <p class="tw-font-semibold tw-text-orange-700">{{ dueSoonCount() }} rapport(s) dus dans 30 jours</p>
              <p class="tw-text-sm tw-text-orange-600">Vérifiez les rappels planifiés.</p>
            </div>
          </div>
        }

      </div>

      <!-- Table -->
      <p-table
        [value]="reports()"
        [lazy]="true"
        [paginator]="true"
        [rows]="pageSize()"
        [totalRecords]="totalRecords()"
        [loading]="isLoading()"
        [rowsPerPageOptions]="[10, 20, 50]"
        (onLazyLoad)="onLazyLoad($event)"
        [sortField]="'dueDate'"
        [defaultSortOrder]="1"
        styleClass="tw-border tw-border-gray-200 tw-rounded-lg"
        [rowHover]="true"
        dataKey="id"
      >
        <ng-template pTemplate="header">
          <tr class="tw-bg-gray-50">
            <th pSortableColumn="studyId" class="tw-font-semibold tw-text-sm">
              Étude <p-sortIcon field="studyId" />
            </th>
            <th pSortableColumn="reportYear" class="tw-font-semibold tw-text-sm">
              Année <p-sortIcon field="reportYear" />
            </th>
            <th pSortableColumn="dueDate" class="tw-font-semibold tw-text-sm">
              Date limite <p-sortIcon field="dueDate" />
            </th>
            <th class="tw-font-semibold tw-text-sm">Date réception</th>
            <th class="tw-font-semibold tw-text-sm">Statut</th>
            <th class="tw-font-semibold tw-text-sm tw-text-center">Rappels</th>
            <th class="tw-font-semibold tw-text-sm tw-text-center">Actions</th>
          </tr>
        </ng-template>

        <ng-template pTemplate="body" let-report>
          <tr>
            <!-- Étude -->
            <td>
              <span class="tw-font-mono tw-text-blue-600 tw-text-xs">
                {{ report.studyId | slice:0:8 }}...
              </span>
            </td>

            <!-- Année -->
            <td class="tw-font-semibold tw-text-gray-800">{{ report.reportYear }}</td>

            <!-- Date limite (colorée selon urgence) -->
            <td>
              <span [class]="getDueDateClass(report)">
                {{ report.dueDate | date:'dd/MM/yyyy' }}
                @if (report.status !== AnnualReportStatus.RECEIVED && report.status !== AnnualReportStatus.REVIEWED && report.status !== AnnualReportStatus.WAIVED) {
                  <span class="tw-text-xs tw-ml-1">({{ report.daysUntilDue > 0 ? 'J-' + report.daysUntilDue : 'J+' + (-report.daysUntilDue) }})</span>
                }
              </span>
            </td>

            <!-- Date réception -->
            <td class="tw-text-sm tw-text-gray-600">
              {{ report.receivedDate ? (report.receivedDate | date:'dd/MM/yyyy') : '—' }}
            </td>

            <!-- Statut -->
            <td>
              <p-tag
                [value]="report.statusLabel"
                [severity]="getStatusSeverity(report.status)"
              />
            </td>

            <!-- Rappels J-60 / J-30 / J-0 -->
            <td class="tw-text-center">
              <div class="tw-flex tw-justify-center tw-gap-2">
                <span pTooltip="Rappel J-60" tooltipPosition="top">
                  @if (report.reminderSent60) {
                    <i class="pi pi-check-circle tw-text-green-500"></i>
                  } @else {
                    <i class="pi pi-circle tw-text-gray-300"></i>
                  }
                </span>
                <span pTooltip="Rappel J-30" tooltipPosition="top">
                  @if (report.reminderSent30) {
                    <i class="pi pi-check-circle tw-text-green-500"></i>
                  } @else {
                    <i class="pi pi-circle tw-text-gray-300"></i>
                  }
                </span>
                <span pTooltip="Rappel J-0" tooltipPosition="top">
                  @if (report.reminderSent0) {
                    <i class="pi pi-check-circle tw-text-green-500"></i>
                  } @else {
                    <i class="pi pi-circle tw-text-gray-300"></i>
                  }
                </span>
              </div>
            </td>

            <!-- Actions -->
            <td class="tw-text-center">
              @if (report.status === AnnualReportStatus.PENDING || report.status === AnnualReportStatus.OVERDUE) {
                <p-button
                  label="Marquer reçu"
                  icon="pi pi-inbox"
                  severity="success"
                  size="small"
                  [text]="true"
                  pTooltip="Marquer le rapport comme reçu"
                  (onClick)="openReceiveDialog(report)"
                />
              } @else {
                <span class="tw-text-gray-300 tw-text-xs">—</span>
              }
            </td>
          </tr>
        </ng-template>

        <ng-template pTemplate="emptymessage">
          <tr>
            <td colspan="7" class="tw-text-center tw-py-12 tw-text-gray-500">
              <i class="pi pi-file tw-text-4xl tw-mb-4 tw-block tw-text-gray-300"></i>
              Aucun rapport annuel trouvé.
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

    <!-- Dialog "Marquer reçu" -->
    <p-dialog
      header="Marquer le rapport comme reçu"
      [(visible)]="showReceiveDialog"
      [modal]="true"
      [style]="{ width: '400px' }"
      [closable]="true"
      [draggable]="false"
    >
      @if (selectedReport()) {
        <div class="tw-space-y-4 tw-pt-2">

          <div class="tw-bg-gray-50 tw-rounded-lg tw-p-3 tw-text-sm tw-space-y-1">
            <p><span class="tw-font-medium">Étude :</span> <span class="tw-font-mono">{{ selectedReport()!.studyId | slice:0:8 }}...</span></p>
            <p><span class="tw-font-medium">Année :</span> {{ selectedReport()!.reportYear }}</p>
            <p><span class="tw-font-medium">Échéance :</span> {{ selectedReport()!.dueDate | date:'dd/MM/yyyy' }}</p>
          </div>

          <div class="tw-flex tw-flex-col tw-gap-1">
            <label class="tw-text-sm tw-font-medium tw-text-gray-700">
              Date de réception <span class="tw-text-red-500">*</span>
            </label>
            <p-calendar
              [(ngModel)]="receiveDate"
              dateFormat="dd/mm/yy"
              [showIcon]="true"
              [maxDate]="today"
              styleClass="tw-w-full"
            />
          </div>

        </div>
      }

      <ng-template pTemplate="footer">
        <div class="tw-flex tw-gap-2 tw-justify-end">
          <p-button
            label="Annuler"
            severity="secondary"
            (onClick)="closeReceiveDialog()"
          />
          <p-button
            label="Confirmer"
            icon="pi pi-check"
            [loading]="isSaving()"
            [disabled]="!receiveDate"
            (onClick)="confirmReceived()"
          />
        </div>
      </ng-template>
    </p-dialog>
  `,
})
export class AnnualReportTrackerComponent implements OnInit {

  private readonly ethicsService = inject(EthicsService);
  private readonly messageService = inject(MessageService);

  /** Expose l'enum AnnualReportStatus au template. */
  protected readonly AnnualReportStatus = AnnualReportStatus;

  /** Date du jour (borne max du calendrier de réception). */
  protected readonly today = new Date();

  // ─── État réactif ──────────────────────────────────────────
  /** Liste des rapports de la page courante. */
  protected readonly reports = signal<AnnualReportResponse[]>([]);
  /** Nombre total de rapports. */
  protected readonly totalRecords = signal(0);
  /** Nombre de rapports en retard. */
  protected readonly overdueCount = signal(0);
  /** Nombre de rapports dus dans les 30 prochains jours. */
  protected readonly dueSoonCount = signal(0);
  /** Indicateur de chargement. */
  protected readonly isLoading = signal(false);
  /** Indicateur de sauvegarde. */
  protected readonly isSaving = signal(false);
  /** Page courante (0-based). */
  private readonly currentPage = signal(0);
  /** Nombre d'éléments par page. */
  protected readonly pageSize = signal(20);

  /** Visibilité du dialog de réception. */
  protected showReceiveDialog = false;
  /** Rapport sélectionné pour marquage. */
  protected readonly selectedReport = signal<AnnualReportResponse | null>(null);
  /** Date de réception saisie dans le dialog. */
  protected receiveDate: Date | null = null;

  // ─────────────────────────────────────────────────────────────

  ngOnInit(): void {
    this.loadReports();
  }

  /**
   * Charge la page de rapports annuels depuis l'API.
   * Met également à jour les compteurs d'alertes.
   */
  protected loadReports(): void {
    this.isLoading.set(true);
    this.ethicsService.getAnnualReports(this.currentPage(), this.pageSize()).subscribe({
      next: (page) => {
        this.reports.set(page.content);
        this.totalRecords.set(page.totalElements);
        this.overdueCount.set(
          page.content.filter(r => r.status === AnnualReportStatus.OVERDUE).length,
        );
        this.dueSoonCount.set(
          page.content.filter(
            r => r.status === AnnualReportStatus.PENDING && r.daysUntilDue <= 30 && r.daysUntilDue >= 0,
          ).length,
        );
        this.isLoading.set(false);
      },
      error: (err) => {
        console.error('Erreur chargement rapports annuels', err);
        this.messageService.add({ severity: 'error', summary: 'Erreur', detail: 'Impossible de charger les rapports.' });
        this.isLoading.set(false);
      },
    });
  }

  /**
   * Ouvre le dialog de marquage "reçu" pour un rapport donné.
   *
   * @param report rapport annuel à marquer comme reçu
   */
  protected openReceiveDialog(report: AnnualReportResponse): void {
    this.selectedReport.set(report);
    this.receiveDate = new Date();
    this.showReceiveDialog = true;
  }

  /** Ferme le dialog et réinitialise l'état. */
  protected closeReceiveDialog(): void {
    this.showReceiveDialog = false;
    this.selectedReport.set(null);
    this.receiveDate = null;
  }

  /**
   * Confirme la réception du rapport et met à jour l'API.
   */
  protected confirmReceived(): void {
    const report = this.selectedReport();
    if (!report || !this.receiveDate) return;

    const receivedDate = this.isoDate(this.receiveDate);
    this.isSaving.set(true);
    this.ethicsService.markReportReceived(report.id, receivedDate).subscribe({
      next: (updated) => {
        this.reports.update(list => list.map(r => r.id === updated.id ? updated : r));
        this.messageService.add({ severity: 'success', summary: 'Succès', detail: 'Rapport marqué comme reçu.' });
        this.isSaving.set(false);
        this.closeReceiveDialog();
        // Recalcul des compteurs
        this.overdueCount.set(this.reports().filter(r => r.status === AnnualReportStatus.OVERDUE).length);
        this.dueSoonCount.set(
          this.reports().filter(r => r.status === AnnualReportStatus.PENDING && r.daysUntilDue <= 30 && r.daysUntilDue >= 0).length,
        );
      },
      error: (err) => {
        console.error('Erreur marquage reçu', err);
        this.messageService.add({ severity: 'error', summary: 'Erreur', detail: 'Impossible de mettre à jour le rapport.' });
        this.isSaving.set(false);
      },
    });
  }

  /**
   * Gère les événements de chargement lazy de la table PrimeNG.
   *
   * @param event événement PrimeNG LazyLoadEvent
   */
  protected onLazyLoad(event: TableLazyLoadEvent): void {
    const rows = event.rows ?? this.pageSize();
    const page = Math.floor((event.first ?? 0) / rows);
    this.currentPage.set(page);
    this.pageSize.set(rows);
    this.loadReports();
  }

  /**
   * Retourne les classes Tailwind pour colorier la date limite selon l'urgence.
   *
   * @param report rapport annuel
   * @returns classes CSS Tailwind
   */
  protected getDueDateClass(report: AnnualReportResponse): string {
    if (report.status === AnnualReportStatus.OVERDUE) {
      return 'tw-text-red-600 tw-font-bold tw-text-sm';
    }
    if (
      report.status === AnnualReportStatus.PENDING &&
      report.daysUntilDue <= 30 &&
      report.daysUntilDue >= 0
    ) {
      return 'tw-text-orange-500 tw-font-semibold tw-text-sm';
    }
    return 'tw-text-gray-700 tw-text-sm';
  }

  /**
   * Retourne la severité PrimeNG Tag pour un statut de rapport annuel.
   *
   * @param status clé du statut AnnualReportStatus
   * @returns severité PrimeNG
   */
  protected getStatusSeverity(status: string): 'success' | 'info' | 'secondary' | 'contrast' | 'warning' | 'danger' | undefined {
    return ANNUAL_REPORT_STATUS_SEVERITY[status] ?? 'info';
  }

  /**
   * Formate un objet Date en chaîne ISO 8601 (yyyy-MM-dd).
   *
   * @param date objet Date
   * @returns chaîne ISO 8601
   */
  private isoDate(date: Date): string {
    const y   = date.getFullYear();
    const m   = String(date.getMonth() + 1).padStart(2, '0');
    const day = String(date.getDate()).padStart(2, '0');
    return `${y}-${m}-${day}`;
  }
}
