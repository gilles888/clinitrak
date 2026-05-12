import { Component, computed, signal } from '@angular/core';
import { PickListModule } from 'primeng/picklist';
import { TableModule } from 'primeng/table';
import { ButtonModule } from 'primeng/button';
import { TagModule } from 'primeng/tag';
import { CardModule } from 'primeng/card';
import { SystemRole } from '../../../core/models/user.model';

/** Représentation simplifiée d'un utilisateur pour l'administration. */
interface AdminUser {
  id: string;
  email: string;
  role: string;
  status: string;
}

/** Rôle disponible pour l'attribution. */
interface RoleOption {
  name: string;
  value: string;
}

/** Rôles disponibles pour l'attribution. */
const ALL_ROLES: RoleOption[] = Object.entries(SystemRole).map(([, value]) => ({
  name: value.replace('ROLE_', ''),
  value,
}));

/** Données mockées d'utilisateurs. */
const MOCK_USERS: AdminUser[] = [
  { id: '1', email: 'admin@clinique.be',       role: SystemRole.ADMIN_TENANT, status: 'ACTIVE' },
  { id: '2', email: 'secretaire@clinique.be',  role: SystemRole.CE_SECRETARY, status: 'ACTIVE' },
  { id: '3', email: 'pharmacien@clinique.be',  role: SystemRole.PHARMACIST,   status: 'ACTIVE' },
  { id: '4', email: 'investigateur@pharma.be', role: SystemRole.INVESTIGATOR, status: 'INACTIVE' },
  { id: '5', email: 'ctc.pm@clinique.be',      role: SystemRole.CTC_PM,       status: 'ACTIVE' },
];

/**
 * Composant de gestion des utilisateurs d'un tenant.
 *
 * <p>Affiche la liste des utilisateurs et permet de gérer leurs rôles
 * via un p-pickList (source = rôles disponibles, target = rôles attribués).
 */
@Component({
  selector: 'app-user-management',
  standalone: true,
  imports: [PickListModule, TableModule, ButtonModule, TagModule, CardModule],
  template: `
    <div class="tw-p-6">

      <!-- Titre -->
      <div class="tw-mb-6">
        <h1 class="tw-text-2xl tw-font-bold tw-text-gray-900">Gestion des utilisateurs</h1>
        <p class="tw-text-sm tw-text-gray-500 tw-mt-1">{{ users().length }} utilisateur(s)</p>
      </div>

      <div class="tw-grid tw-grid-cols-1 xl:tw-grid-cols-2 tw-gap-6">

        <!-- Tableau utilisateurs -->
        <div>
          <p-card header="Utilisateurs">
            <p-table
              [value]="users()"
              styleClass="tw-text-sm"
              selectionMode="single"
              (onRowSelect)="onUserSelect($event)"
            >
              <ng-template pTemplate="header">
                <tr>
                  <th class="tw-text-left">Email</th>
                  <th class="tw-text-left">Rôle</th>
                  <th class="tw-text-left">Statut</th>
                </tr>
              </ng-template>
              <ng-template pTemplate="body" let-user>
                <tr
                  class="tw-cursor-pointer"
                  [class.tw-bg-blue-50]="selectedUser() === user.id"
                >
                  <td class="tw-text-gray-800">{{ user.email }}</td>
                  <td>
                    <p-tag [value]="formatRole(user.role)" severity="info" styleClass="tw-text-xs" />
                  </td>
                  <td>
                    <p-tag
                      [value]="user.status"
                      [severity]="user.status === 'ACTIVE' ? 'success' : 'secondary'"
                      styleClass="tw-text-xs"
                    />
                  </td>
                </tr>
              </ng-template>
            </p-table>
          </p-card>
        </div>

        <!-- PickList rôles -->
        <div>
          <p-card [header]="selectedUser() ? 'Rôles — ' + getSelectedUserEmail() : 'Rôles (sélectionnez un utilisateur)'">
            @if (!selectedUser()) {
              <div class="tw-text-center tw-py-8 tw-text-gray-400">
                <i class="pi pi-arrow-left tw-text-3xl tw-block tw-mb-2"></i>
                <p class="tw-text-sm">Sélectionnez un utilisateur pour gérer ses rôles</p>
              </div>
            } @else {
              <p-pickList
                [source]="availableRoles()"
                [target]="assignedRoles()"
                sourceHeader="Rôles disponibles"
                targetHeader="Rôles attribués"
                [dragdrop]="true"
                [responsive]="true"
                filterBy="name"
                sourceFilterPlaceholder="Rechercher..."
                targetFilterPlaceholder="Rechercher..."
                (onMoveToTarget)="onRoleAssigned($event)"
                (onMoveToSource)="onRoleRemoved($event)"
              >
                <ng-template let-role pTemplate="item">
                  <div class="tw-flex tw-items-center tw-gap-2 tw-py-1">
                    <i class="pi pi-shield tw-text-blue-400 tw-text-xs"></i>
                    <span class="tw-text-sm tw-font-mono">{{ role.name }}</span>
                  </div>
                </ng-template>
              </p-pickList>

              <div class="tw-mt-4 tw-flex tw-justify-end">
                <p-button
                  label="Enregistrer les rôles"
                  icon="pi pi-save"
                  (onClick)="saveRoles()"
                />
              </div>
            }
          </p-card>
        </div>

      </div>
    </div>
  `,
})
export class UserManagementComponent {

  /** Liste des utilisateurs (données mockées). */
  protected readonly users = signal<AdminUser[]>(MOCK_USERS);

  /** Identifiant de l'utilisateur sélectionné. */
  protected readonly selectedUser = signal<string | null>(null);

  /** Rôles disponibles (non encore attribués). */
  protected readonly availableRoles = signal<RoleOption[]>([...ALL_ROLES]);

  /** Rôles attribués à l'utilisateur sélectionné. */
  protected readonly assignedRoles = signal<RoleOption[]>([]);

  /**
   * Sélectionne un utilisateur et initialise les pickLists de rôles.
   *
   * @param event Événement de sélection de ligne
   */
  protected onUserSelect(event: { data: AdminUser }): void {
    const user = event.data;
    this.selectedUser.set(user.id);

    const assigned = ALL_ROLES.filter(r => r.value === user.role);
    const available = ALL_ROLES.filter(r => r.value !== user.role);

    this.assignedRoles.set([...assigned]);
    this.availableRoles.set([...available]);
  }

  /**
   * Callback lors du déplacement d'un rôle vers la cible.
   *
   * @param event Événement PrimeNG pickList
   */
  protected onRoleAssigned(event: { items: RoleOption[] }): void {
    const current = this.assignedRoles();
    this.assignedRoles.set([...current, ...event.items]);
    this.availableRoles.update(r => r.filter(x => !event.items.find(i => i.value === x.value)));
  }

  /**
   * Callback lors du retrait d'un rôle.
   *
   * @param event Événement PrimeNG pickList
   */
  protected onRoleRemoved(event: { items: RoleOption[] }): void {
    const current = this.availableRoles();
    this.availableRoles.set([...current, ...event.items]);
    this.assignedRoles.update(r => r.filter(x => !event.items.find(i => i.value === x.value)));
  }

  /**
   * Sauvegarde les rôles attribués (simulation).
   */
  protected saveRoles(): void {
    // Ici, on enverrait l'appel API avec les rôles assignés
    console.info('Rôles à enregistrer pour', this.selectedUser(), ':', this.assignedRoles().map(r => r.value));
  }

  /**
   * Retourne l'email de l'utilisateur sélectionné.
   */
  protected getSelectedUserEmail(): string {
    return this.users().find(u => u.id === this.selectedUser())?.email ?? '';
  }

  /**
   * Formate un rôle pour l'affichage (supprime le préfixe ROLE_).
   *
   * @param role Valeur du rôle
   */
  protected formatRole(role: string): string {
    return role.replace('ROLE_', '');
  }
}
