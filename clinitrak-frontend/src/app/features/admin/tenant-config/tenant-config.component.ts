import { Component, inject, input, OnInit, signal } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { RouterLink } from '@angular/router';
import { InputTextModule } from 'primeng/inputtext';
import { InputNumberModule } from 'primeng/inputnumber';
import { MultiSelectModule } from 'primeng/multiselect';
import { ButtonModule } from 'primeng/button';
import { DialogModule } from 'primeng/dialog';
import { CardModule } from 'primeng/card';
import { MessagesModule } from 'primeng/messages';
import { Message } from 'primeng/api';
import { AdminService } from '../../../core/services/admin.service';
import {
  AdminTenant,
  MODULE_OPTIONS,
  ModuleType,
  TenantStatistics,
} from '../../../core/models/admin.model';

/**
 * Page de configuration détaillée d'un tenant.
 *
 * <p>Permet de modifier les paramètres de configuration (format numéro CE,
 * timezone, langue, quotas), les modules actifs et d'inviter des utilisateurs.
 * Affiche également les statistiques d'utilisation du tenant.
 */
@Component({
  selector: 'app-tenant-config',
  standalone: true,
  imports: [
    ReactiveFormsModule,
    RouterLink,
    InputTextModule,
    InputNumberModule,
    MultiSelectModule,
    ButtonModule,
    DialogModule,
    CardModule,
    MessagesModule,
  ],
  template: `
    <div class="tw-p-6 tw-max-w-5xl tw-mx-auto">

      <!-- Navigation -->
      <div class="tw-mb-6">
        <a routerLink="/admin/tenants" class="tw-text-blue-600 hover:tw-underline tw-text-sm">
          &larr; Retour à la liste des tenants
        </a>
        <h1 class="tw-text-2xl tw-font-bold tw-text-gray-900 tw-mt-1">
          Configuration — {{ tenant()?.name ?? 'Chargement...' }}
        </h1>
        @if (tenant()) {
          <p class="tw-text-sm tw-text-gray-500">Slug : {{ tenant()!.slug }}</p>
        }
      </div>

      <!-- Messages -->
      @if (messages().length > 0) {
        <p-messages [value]="messages()" styleClass="tw-mb-4" />
      }

      @if (isLoading() && !tenant()) {
        <div class="tw-text-center tw-py-16 tw-text-gray-400">
          <i class="pi pi-spin pi-spinner tw-text-4xl"></i>
        </div>
      } @else if (tenant()) {

        <div class="tw-grid tw-grid-cols-1 lg:tw-grid-cols-3 tw-gap-6">

          <!-- Statistiques -->
          <div class="lg:tw-col-span-3 tw-grid tw-grid-cols-3 tw-gap-4">
            <p-card styleClass="tw-text-center">
              <p class="tw-text-3xl tw-font-bold tw-text-blue-600">{{ stats()?.userCount ?? 0 }}</p>
              <p class="tw-text-sm tw-text-gray-500 tw-mt-1">Utilisateurs</p>
            </p-card>
            <p-card styleClass="tw-text-center">
              <p class="tw-text-3xl tw-font-bold tw-text-purple-600">{{ stats()?.studyCount ?? 0 }}</p>
              <p class="tw-text-sm tw-text-gray-500 tw-mt-1">Études totales</p>
            </p-card>
            <p-card styleClass="tw-text-center">
              <p class="tw-text-3xl tw-font-bold tw-text-green-600">{{ stats()?.activeStudies ?? 0 }}</p>
              <p class="tw-text-sm tw-text-gray-500 tw-mt-1">Études actives</p>
            </p-card>
          </div>

          <!-- Formulaire configuration -->
          <div class="lg:tw-col-span-2">
            <p-card header="Paramètres de configuration">
              <form [formGroup]="configForm" (ngSubmit)="saveConfig()" class="tw-space-y-5">

                <div class="tw-flex tw-flex-col tw-gap-1.5">
                  <label class="tw-text-sm tw-font-medium tw-text-gray-700">Format numéro CE</label>
                  <input
                    pInputText
                    formControlName="ceNumberFormat"
                    placeholder="CE-{YEAR}-{SEQ}"
                    class="tw-w-full"
                  />
                </div>

                <div class="tw-flex tw-flex-col tw-gap-1.5">
                  <label class="tw-text-sm tw-font-medium tw-text-gray-700">Fuseau horaire</label>
                  <input
                    pInputText
                    formControlName="timezone"
                    placeholder="Europe/Brussels"
                    class="tw-w-full"
                  />
                </div>

                <div class="tw-flex tw-flex-col tw-gap-1.5">
                  <label class="tw-text-sm tw-font-medium tw-text-gray-700">Langue par défaut</label>
                  <input
                    pInputText
                    formControlName="defaultLanguage"
                    placeholder="fr"
                    class="tw-w-full"
                  />
                </div>

                <div class="tw-grid tw-grid-cols-2 tw-gap-4">
                  <div class="tw-flex tw-flex-col tw-gap-1.5">
                    <label class="tw-text-sm tw-font-medium tw-text-gray-700">Nb utilisateurs max</label>
                    <p-inputNumber
                      formControlName="maxUsers"
                      [min]="1"
                      placeholder="50"
                      styleClass="tw-w-full"
                      inputStyleClass="tw-w-full"
                    />
                  </div>
                  <div class="tw-flex tw-flex-col tw-gap-1.5">
                    <label class="tw-text-sm tw-font-medium tw-text-gray-700">Quota stockage (Go)</label>
                    <p-inputNumber
                      formControlName="storageQuotaGb"
                      [min]="1"
                      placeholder="100"
                      styleClass="tw-w-full"
                      inputStyleClass="tw-w-full"
                    />
                  </div>
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

                <p-button
                  type="submit"
                  label="Enregistrer la configuration"
                  icon="pi pi-save"
                  styleClass="tw-w-full"
                  [loading]="isSaving()"
                />
              </form>
            </p-card>
          </div>

          <!-- Inviter un utilisateur -->
          <div>
            <p-card header="Invitations">
              <p class="tw-text-sm tw-text-gray-600 tw-mb-4">
                Invitez un utilisateur à rejoindre ce tenant.
              </p>
              <p-button
                label="Inviter un utilisateur"
                icon="pi pi-user-plus"
                styleClass="tw-w-full"
                (onClick)="showInviteDialog = true"
              />

              @if (lastInvite()) {
                <div class="tw-mt-4 tw-p-3 tw-bg-green-50 tw-border tw-border-green-200 tw-rounded-lg tw-text-sm">
                  <p class="tw-font-medium tw-text-green-700">Invitation envoyée !</p>
                  <p class="tw-text-green-600">{{ lastInvite()!.email }}</p>
                </div>
              }
            </p-card>
          </div>

        </div>
      }

      <!-- Dialog invitation -->
      <p-dialog
        header="Inviter un utilisateur"
        [(visible)]="showInviteDialog"
        [modal]="true"
        [style]="{ width: '480px' }"
        [draggable]="false"
      >
        <form [formGroup]="inviteForm" (ngSubmit)="submitInvite()" class="tw-space-y-4 tw-pt-2">

          <div class="tw-flex tw-flex-col tw-gap-1.5">
            <label class="tw-text-sm tw-font-medium tw-text-gray-700">Email *</label>
            <input pInputText formControlName="email" type="email" placeholder="user@institution.be" class="tw-w-full" />
          </div>

          <div class="tw-grid tw-grid-cols-2 tw-gap-4">
            <div class="tw-flex tw-flex-col tw-gap-1.5">
              <label class="tw-text-sm tw-font-medium tw-text-gray-700">Prénom</label>
              <input pInputText formControlName="firstName" placeholder="Prénom" class="tw-w-full" />
            </div>
            <div class="tw-flex tw-flex-col tw-gap-1.5">
              <label class="tw-text-sm tw-font-medium tw-text-gray-700">Nom</label>
              <input pInputText formControlName="lastName" placeholder="Nom" class="tw-w-full" />
            </div>
          </div>

          <div class="tw-flex tw-flex-col tw-gap-1.5">
            <label class="tw-text-sm tw-font-medium tw-text-gray-700">Rôle</label>
            <input pInputText formControlName="role" placeholder="ROLE_CTC_DESK" class="tw-w-full" />
          </div>

        </form>

        <ng-template pTemplate="footer">
          <p-button label="Annuler" severity="secondary" (onClick)="showInviteDialog = false" />
          <p-button
            label="Envoyer l'invitation"
            icon="pi pi-send"
            [loading]="isInviting()"
            [disabled]="inviteForm.invalid || isInviting()"
            (onClick)="submitInvite()"
          />
        </ng-template>
      </p-dialog>

    </div>
  `,
})
export class TenantConfigComponent implements OnInit {

  private readonly adminService = inject(AdminService);
  private readonly fb = inject(FormBuilder);

  /** Identifiant du tenant depuis la route (paramètre :id). */
  readonly id = input.required<string>();

  /** Tenant courant. */
  protected readonly tenant = signal<AdminTenant | null>(null);

  /** Statistiques du tenant courant. */
  protected readonly stats = signal<TenantStatistics | null>(null);

  /** Indique si le chargement est en cours. */
  protected readonly isLoading = signal(false);

  /** Indique si la sauvegarde de config est en cours. */
  protected readonly isSaving = signal(false);

  /** Indique si l'invitation est en cours. */
  protected readonly isInviting = signal(false);

  /** Dernière invitation envoyée. */
  protected readonly lastInvite = signal<{ email: string; status: string } | null>(null);

  /** Messages informatifs. */
  protected readonly messages = signal<Message[]>([]);

  protected showInviteDialog = false;

  protected readonly moduleOptions = MODULE_OPTIONS;

  protected readonly configForm = this.fb.group({
    ceNumberFormat:  [''],
    timezone:        [''],
    defaultLanguage: [''],
    maxUsers:        [null as number | null],
    storageQuotaGb:  [null as number | null],
    activeModules:   [[] as ModuleType[]],
  });

  protected readonly inviteForm = this.fb.group({
    email:     ['', [Validators.required, Validators.email]],
    firstName: [''],
    lastName:  [''],
    role:      [''],
  });

  /** Charge le tenant et ses statistiques. */
  ngOnInit(): void {
    this.isLoading.set(true);
    const id = this.id();

    this.adminService.getTenantStatistics(id).subscribe({
      next: (s) => this.stats.set(s),
      error: () => {},
    });

    // Charge depuis la liste (pas d'endpoint getById documenté — utilise getTenants)
    this.adminService.getTenants().subscribe({
      next: (list) => {
        const t = list.find(x => x.id === id) ?? null;
        this.tenant.set(t);
        if (t) {
          this.configForm.patchValue({
            activeModules: t.activeModules,
          });
        }
        this.isLoading.set(false);
      },
      error: () => this.isLoading.set(false),
    });
  }

  /**
   * Sauvegarde la configuration du tenant.
   */
  protected saveConfig(): void {
    if (!this.tenant()) return;
    this.isSaving.set(true);
    this.messages.set([]);

    this.adminService.updateTenantConfig(this.tenant()!.id, this.configForm.value as Record<string, unknown>).subscribe({
      next: (updated) => {
        this.tenant.set(updated);
        this.isSaving.set(false);
        this.messages.set([{ severity: 'success', summary: 'Succès', detail: 'Configuration enregistrée.' }]);
      },
      error: () => {
        this.isSaving.set(false);
        this.messages.set([{ severity: 'error', summary: 'Erreur', detail: 'Impossible d\'enregistrer la configuration.' }]);
      },
    });
  }

  /**
   * Envoie une invitation à un utilisateur.
   */
  protected submitInvite(): void {
    if (this.inviteForm.invalid || !this.tenant()) return;
    this.isInviting.set(true);

    this.adminService.inviteUser(this.tenant()!.id, this.inviteForm.value as Record<string, unknown>).subscribe({
      next: (result) => {
        this.lastInvite.set(result);
        this.isInviting.set(false);
        this.showInviteDialog = false;
        this.inviteForm.reset();
      },
      error: () => this.isInviting.set(false),
    });
  }
}
