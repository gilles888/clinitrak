import { Component, inject, OnInit, signal } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { SlicePipe } from '@angular/common';
import { RouterLink } from '@angular/router';
import { TableModule } from 'primeng/table';
import { TagModule } from 'primeng/tag';
import { ButtonModule } from 'primeng/button';
import { DialogModule } from 'primeng/dialog';
import { InputTextModule } from 'primeng/inputtext';
import { DropdownModule } from 'primeng/dropdown';
import { MultiSelectModule } from 'primeng/multiselect';
import { CheckboxModule } from 'primeng/checkbox';
import { AdminService } from '../../../core/services/admin.service';
import {
  AdminTenant,
  MODULE_OPTIONS,
  ModuleType,
  SUBSCRIPTION_OPTIONS,
  SUBSCRIPTION_SEVERITY,
  TENANT_STATUS_SEVERITY,
  TenantStatus,
  SubscriptionType,
} from '../../../core/models/admin.model';

/**
 * Liste et gestion des tenants de la plateforme CliniTrak.
 *
 * <p>Permet la création de nouveaux tenants via un dialog ReactiveForm
 * et l'accès à la configuration de chaque tenant.
 */
@Component({
  selector: 'app-tenant-list',
  standalone: true,
  imports: [
    ReactiveFormsModule,
    SlicePipe,
    RouterLink,
    TableModule,
    TagModule,
    ButtonModule,
    DialogModule,
    InputTextModule,
    DropdownModule,
    MultiSelectModule,
    CheckboxModule,
  ],
  template: `
    <div class="tw-p-6">

      <!-- En-tête -->
      <div class="tw-flex tw-justify-between tw-items-center tw-mb-6">
        <div>
          <h1 class="tw-text-2xl tw-font-bold tw-text-gray-900">Gestion des tenants</h1>
          <p class="tw-text-sm tw-text-gray-500 tw-mt-1">{{ tenants().length }} tenant(s) enregistré(s)</p>
        </div>
        <p-button
          label="Nouveau tenant"
          icon="pi pi-plus"
          (onClick)="openCreateDialog()"
        />
      </div>

      <!-- Tableau -->
      <div class="tw-bg-white tw-rounded-xl tw-shadow tw-overflow-hidden">
        <p-table
          [value]="tenants()"
          [loading]="isLoading()"
          [paginator]="true"
          [rows]="15"
          styleClass="tw-text-sm"
        >
          <ng-template pTemplate="header">
            <tr>
              <th class="tw-text-left">Nom</th>
              <th class="tw-text-left">Slug</th>
              <th class="tw-text-left">Domaine</th>
              <th class="tw-text-left">Abonnement</th>
              <th class="tw-text-left">Modules actifs</th>
              <th class="tw-text-left">Statut</th>
              <th class="tw-text-left">Créé le</th>
              <th></th>
            </tr>
          </ng-template>
          <ng-template pTemplate="body" let-tenant>
            <tr>
              <td class="tw-font-semibold tw-text-gray-800">{{ tenant.name }}</td>
              <td class="tw-font-mono tw-text-xs tw-text-gray-500">{{ tenant.slug }}</td>
              <td class="tw-text-gray-600">{{ tenant.domain || '—' }}</td>
              <td>
                <p-tag
                  [value]="tenant.subscriptionTypeLabel"
                  [severity]="getSubscriptionSeverity(tenant.subscriptionType)"
                />
              </td>
              <td>
                <div class="tw-flex tw-flex-wrap tw-gap-1">
                  @for (mod of tenant.activeModules; track mod) {
                    <p-tag [value]="mod" severity="info" styleClass="tw-text-xs" />
                  }
                </div>
              </td>
              <td>
                <p-tag
                  [value]="tenant.statusLabel"
                  [severity]="getStatusSeverity(tenant.status)"
                />
              </td>
              <td class="tw-text-gray-500 tw-text-xs">{{ tenant.createdAt | slice:0:10 }}</td>
              <td>
                <a
                  [routerLink]="['/admin/tenants', tenant.id, 'config']"
                  pButton
                  label="Configurer"
                  icon="pi pi-cog"
                  severity="secondary"
                  size="small"
                ></a>
              </td>
            </tr>
          </ng-template>
          <ng-template pTemplate="emptymessage">
            <tr>
              <td colspan="8" class="tw-text-center tw-py-8 tw-text-gray-400">
                Aucun tenant enregistré.
              </td>
            </tr>
          </ng-template>
        </p-table>
      </div>

      <!-- Dialog création -->
      <p-dialog
        header="Créer un nouveau tenant"
        [(visible)]="showDialog"
        [modal]="true"
        [style]="{ width: '560px' }"
        [draggable]="false"
      >
        <form [formGroup]="createForm" (ngSubmit)="submitCreate()" class="tw-space-y-4 tw-pt-2">

          <div class="tw-flex tw-flex-col tw-gap-1.5">
            <label class="tw-text-sm tw-font-medium tw-text-gray-700">Nom *</label>
            <input pInputText formControlName="name" placeholder="Clinique XYZ" class="tw-w-full" />
            @if (createForm.controls.name.dirty && createForm.controls.name.errors?.['required']) {
              <small class="tw-text-red-500">Obligatoire</small>
            }
          </div>

          <div class="tw-flex tw-flex-col tw-gap-1.5">
            <label class="tw-text-sm tw-font-medium tw-text-gray-700">Slug *</label>
            <input pInputText formControlName="slug" placeholder="clinique-xyz" class="tw-w-full" />
            <small class="tw-text-gray-400">Identifiant unique URL-friendly (minuscules, tirets)</small>
          </div>

          <div class="tw-flex tw-flex-col tw-gap-1.5">
            <label class="tw-text-sm tw-font-medium tw-text-gray-700">Domaine</label>
            <input pInputText formControlName="domain" placeholder="clinique-xyz.clinitrak.be" class="tw-w-full" />
          </div>

          <div class="tw-flex tw-flex-col tw-gap-1.5">
            <label class="tw-text-sm tw-font-medium tw-text-gray-700">Modules actifs</label>
            <p-multiSelect
              formControlName="activeModules"
              [options]="moduleOptions"
              optionLabel="label"
              optionValue="value"
              placeholder="Sélectionner les modules"
              styleClass="tw-w-full"
            />
          </div>

          <div class="tw-flex tw-flex-col tw-gap-1.5">
            <label class="tw-text-sm tw-font-medium tw-text-gray-700">Type d'abonnement *</label>
            <p-dropdown
              formControlName="subscriptionType"
              [options]="subscriptionOptions"
              optionLabel="label"
              optionValue="value"
              placeholder="Sélectionner"
              styleClass="tw-w-full"
            />
          </div>

        </form>

        <ng-template pTemplate="footer">
          <p-button label="Annuler" severity="secondary" (onClick)="showDialog = false" />
          <p-button
            label="Créer le tenant"
            icon="pi pi-check"
            [loading]="isSaving()"
            [disabled]="createForm.invalid || isSaving()"
            (onClick)="submitCreate()"
          />
        </ng-template>
      </p-dialog>

    </div>
  `,
})
export class TenantListComponent implements OnInit {

  private readonly adminService = inject(AdminService);
  private readonly fb = inject(FormBuilder);

  /** Liste des tenants. */
  protected readonly tenants = signal<AdminTenant[]>([]);

  /** Indique si le chargement est en cours. */
  protected readonly isLoading = signal(false);

  /** Indique si la sauvegarde est en cours. */
  protected readonly isSaving = signal(false);

  /** Contrôle la visibilité du dialog. */
  protected showDialog = false;

  protected readonly moduleOptions = MODULE_OPTIONS;
  protected readonly subscriptionOptions = SUBSCRIPTION_OPTIONS;

  protected readonly createForm = this.fb.group({
    name:             ['', Validators.required],
    slug:             ['', Validators.required],
    domain:           [''],
    activeModules:    [[] as ModuleType[]],
    subscriptionType: ['', Validators.required],
  });

  /** Sévérité PrimeNG selon le statut du tenant. */
  protected getStatusSeverity(status: TenantStatus): string {
    return TENANT_STATUS_SEVERITY[status] ?? 'secondary';
  }

  /** Sévérité PrimeNG selon le type d'abonnement. */
  protected getSubscriptionSeverity(sub: SubscriptionType): string {
    return SUBSCRIPTION_SEVERITY[sub] ?? 'secondary';
  }

  /** Charge les tenants au démarrage. */
  ngOnInit(): void {
    this.loadTenants();
  }

  /** Récupère la liste des tenants depuis l'API. */
  private loadTenants(): void {
    this.isLoading.set(true);
    this.adminService.getTenants().subscribe({
      next: (data) => {
        this.tenants.set(data);
        this.isLoading.set(false);
      },
      error: () => this.isLoading.set(false),
    });
  }

  /** Ouvre le dialog de création et réinitialise le formulaire. */
  protected openCreateDialog(): void {
    this.createForm.reset({ activeModules: [], subscriptionType: '' });
    this.showDialog = true;
  }

  /**
   * Soumet le formulaire de création de tenant.
   */
  protected submitCreate(): void {
    if (this.createForm.invalid) return;

    this.isSaving.set(true);
    this.adminService.createTenant(this.createForm.value as Partial<AdminTenant>).subscribe({
      next: (tenant) => {
        this.tenants.update(list => [...list, tenant]);
        this.isSaving.set(false);
        this.showDialog = false;
      },
      error: () => this.isSaving.set(false),
    });
  }
}
