import { Component, computed, inject, OnInit, signal } from '@angular/core';
import { SlicePipe } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { TableModule } from 'primeng/table';
import { ButtonModule } from 'primeng/button';
import { InputTextModule } from 'primeng/inputtext';
import { CalendarModule } from 'primeng/calendar';
import { DropdownModule } from 'primeng/dropdown';
import { CardModule } from 'primeng/card';
import { TagModule } from 'primeng/tag';
import { AdminService } from '../../../core/services/admin.service';
import { AuditLog } from '../../../core/models/admin.model';

/** Options pour le filtre d'action. */
const ACTION_OPTIONS = [
  { label: 'Toutes les actions', value: '' },
  { label: 'CREATE', value: 'CREATE' },
  { label: 'UPDATE', value: 'UPDATE' },
  { label: 'DELETE', value: 'DELETE' },
  { label: 'LOGIN',  value: 'LOGIN' },
  { label: 'LOGOUT', value: 'LOGOUT' },
  { label: 'EXPORT', value: 'EXPORT' },
];

/**
 * Visionneuse du journal d'audit système.
 *
 * <p>Affiche les entrées d'audit avec filtrage par tenant, action et plage de dates.
 * Permet l'export Excel via un téléchargement de blob binaire.
 */
@Component({
  selector: 'app-audit-log-viewer',
  standalone: true,
  imports: [
    SlicePipe,
    FormsModule,
    TableModule,
    ButtonModule,
    InputTextModule,
    CalendarModule,
    DropdownModule,
    CardModule,
    TagModule,
  ],
  template: `
    <div class="tw-p-6">

      <!-- En-tête -->
      <div class="tw-flex tw-justify-between tw-items-center tw-mb-6">
        <div>
          <h1 class="tw-text-2xl tw-font-bold tw-text-gray-900">Journal d'audit</h1>
          <p class="tw-text-sm tw-text-gray-500 tw-mt-1">
            {{ filteredLogs().length }} entrée(s) sur {{ logs().length }} au total
          </p>
        </div>
        <div class="tw-flex tw-gap-2">
          <p-button
            label="Actualiser"
            icon="pi pi-refresh"
            severity="secondary"
            [loading]="isLoading()"
            (onClick)="loadLogs()"
          />
          <p-button
            label="Exporter Excel"
            icon="pi pi-file-excel"
            severity="success"
            [loading]="isExporting()"
            (onClick)="exportLogs()"
          />
        </div>
      </div>

      <!-- Filtres -->
      <p-card styleClass="tw-mb-4">
        <div class="tw-grid tw-grid-cols-1 md:tw-grid-cols-4 tw-gap-4">

          <div class="tw-flex tw-flex-col tw-gap-1.5">
            <label class="tw-text-xs tw-font-medium tw-text-gray-600">Tenant</label>
            <input
              pInputText
              [(ngModel)]="filterTenantValue"
              (ngModelChange)="filterTenant.set($event)"
              placeholder="ID ou nom du tenant"
              class="tw-w-full tw-text-sm"
            />
          </div>

          <div class="tw-flex tw-flex-col tw-gap-1.5">
            <label class="tw-text-xs tw-font-medium tw-text-gray-600">Action</label>
            <p-dropdown
              [(ngModel)]="filterActionValue"
              (ngModelChange)="filterAction.set($event)"
              [options]="actionOptions"
              optionLabel="label"
              optionValue="value"
              styleClass="tw-w-full tw-text-sm"
            />
          </div>

          <div class="tw-flex tw-flex-col tw-gap-1.5">
            <label class="tw-text-xs tw-font-medium tw-text-gray-600">Du</label>
            <p-calendar
              [(ngModel)]="filterFromValue"
              (ngModelChange)="filterFrom.set($event)"
              dateFormat="dd/mm/yy"
              placeholder="Date début"
              styleClass="tw-w-full tw-text-sm"
              [showClear]="true"
            />
          </div>

          <div class="tw-flex tw-flex-col tw-gap-1.5">
            <label class="tw-text-xs tw-font-medium tw-text-gray-600">Au</label>
            <p-calendar
              [(ngModel)]="filterToValue"
              (ngModelChange)="filterTo.set($event)"
              dateFormat="dd/mm/yy"
              placeholder="Date fin"
              styleClass="tw-w-full tw-text-sm"
              [showClear]="true"
            />
          </div>

        </div>
        <div class="tw-mt-3 tw-flex tw-justify-end">
          <p-button
            label="Réinitialiser les filtres"
            severity="secondary"
            size="small"
            icon="pi pi-times"
            (onClick)="resetFilters()"
          />
        </div>
      </p-card>

      <!-- Tableau -->
      <div class="tw-bg-white tw-rounded-xl tw-shadow tw-overflow-hidden">
        <p-table
          [value]="filteredLogs()"
          [loading]="isLoading()"
          [paginator]="true"
          [rows]="20"
          [rowsPerPageOptions]="[10, 20, 50, 100]"
          styleClass="tw-text-sm"
        >
          <ng-template pTemplate="header">
            <tr>
              <th class="tw-text-left">Timestamp</th>
              <th class="tw-text-left">Tenant</th>
              <th class="tw-text-left">Utilisateur</th>
              <th class="tw-text-left">Action</th>
              <th class="tw-text-left">Entité</th>
              <th class="tw-text-left">Ancienne valeur</th>
              <th class="tw-text-left">IP</th>
            </tr>
          </ng-template>
          <ng-template pTemplate="body" let-log>
            <tr>
              <td class="tw-text-gray-500 tw-text-xs tw-whitespace-nowrap">
                {{ log.timestamp | slice:0:19 }}
              </td>
              <td class="tw-font-mono tw-text-xs tw-text-gray-600">{{ log.tenantId | slice:0:8 }}…</td>
              <td class="tw-text-gray-700 tw-text-xs tw-font-mono">{{ log.userId | slice:0:8 }}…</td>
              <td>
                <p-tag
                  [value]="log.action"
                  [severity]="getActionSeverity(log.action)"
                  styleClass="tw-text-xs"
                />
              </td>
              <td class="tw-text-gray-600 tw-text-xs">
                @if (log.entityType) {
                  {{ log.entityType }}
                  @if (log.entityId) {
                    <span class="tw-text-gray-400"> #{{ log.entityId | slice:0:6 }}</span>
                  }
                } @else {
                  <span class="tw-text-gray-300">—</span>
                }
              </td>
              <td class="tw-text-gray-500 tw-text-xs tw-max-w-xs tw-truncate" [title]="log.oldValue ?? ''">
                {{ log.oldValue ? (log.oldValue | slice:0:50) + (log.oldValue.length > 50 ? '…' : '') : '—' }}
              </td>
              <td class="tw-text-gray-400 tw-text-xs tw-font-mono">{{ log.ipAddress ?? '—' }}</td>
            </tr>
          </ng-template>
          <ng-template pTemplate="emptymessage">
            <tr>
              <td colspan="7" class="tw-text-center tw-py-8 tw-text-gray-400">
                Aucune entrée d'audit correspondant aux critères.
              </td>
            </tr>
          </ng-template>
        </p-table>
      </div>

    </div>
  `,
})
export class AuditLogViewerComponent implements OnInit {

  private readonly adminService = inject(AdminService);

  /** Toutes les entrées d'audit chargées. */
  protected readonly logs = signal<AuditLog[]>([]);

  /** Indique si le chargement est en cours. */
  protected readonly isLoading = signal(false);

  /** Indique si l'export est en cours. */
  protected readonly isExporting = signal(false);

  // Filtres (signals)
  protected readonly filterTenant = signal('');
  protected readonly filterAction = signal('');
  protected readonly filterFrom = signal<Date | null>(null);
  protected readonly filterTo = signal<Date | null>(null);

  // Liaisons ngModel
  protected filterTenantValue = '';
  protected filterActionValue = '';
  protected filterFromValue: Date | null = null;
  protected filterToValue: Date | null = null;

  protected readonly actionOptions = ACTION_OPTIONS;

  /**
   * Logs filtrés selon les critères courants.
   * Recalculé automatiquement à chaque changement de filtre.
   */
  protected readonly filteredLogs = computed(() => {
    const tenant = this.filterTenant().toLowerCase();
    const action = this.filterAction();
    const from   = this.filterFrom();
    const to     = this.filterTo();

    return this.logs().filter(log => {
      if (tenant && !log.tenantId.toLowerCase().includes(tenant)) return false;
      if (action && log.action !== action) return false;
      if (from) {
        const ts = new Date(log.timestamp);
        if (ts < from) return false;
      }
      if (to) {
        const ts = new Date(log.timestamp);
        const endOfDay = new Date(to);
        endOfDay.setHours(23, 59, 59, 999);
        if (ts > endOfDay) return false;
      }
      return true;
    });
  });

  /** Charge les logs au démarrage. */
  ngOnInit(): void {
    this.loadLogs();
  }

  /** Récupère les entrées d'audit depuis l'API. */
  protected loadLogs(): void {
    this.isLoading.set(true);
    this.adminService.getAuditLogs().subscribe({
      next: (data) => {
        this.logs.set(data);
        this.isLoading.set(false);
      },
      error: () => this.isLoading.set(false),
    });
  }

  /**
   * Exporte le journal d'audit en Excel et déclenche le téléchargement.
   */
  protected exportLogs(): void {
    this.isExporting.set(true);
    this.adminService.exportAuditLogs().subscribe({
      next: (blob) => {
        const url = URL.createObjectURL(blob);
        const a = document.createElement('a');
        a.href = url;
        a.download = `audit-logs-${new Date().toISOString().slice(0, 10)}.xlsx`;
        a.click();
        URL.revokeObjectURL(url);
        this.isExporting.set(false);
      },
      error: () => this.isExporting.set(false),
    });
  }

  /** Remet tous les filtres à zéro. */
  protected resetFilters(): void {
    this.filterTenantValue = '';
    this.filterActionValue = '';
    this.filterFromValue   = null;
    this.filterToValue     = null;
    this.filterTenant.set('');
    this.filterAction.set('');
    this.filterFrom.set(null);
    this.filterTo.set(null);
  }

  /**
   * Retourne la sévérité PrimeNG selon l'action d'audit.
   *
   * @param action Type d'action
   */
  protected getActionSeverity(action: string): 'success' | 'info' | 'secondary' | 'contrast' | 'warning' | 'danger' | undefined {
    switch (action) {
      case 'CREATE': return 'success';
      case 'UPDATE': return 'info';
      case 'DELETE': return 'danger';
      case 'LOGIN':  return 'secondary';
      case 'EXPORT': return 'warning';
      default:       return 'secondary';
    }
  }
}
