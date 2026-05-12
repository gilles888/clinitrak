import { Component, computed, inject, signal } from '@angular/core';
import { RouterLink } from '@angular/router';
import { CommonModule } from '@angular/common';
import { ButtonModule } from 'primeng/button';
import { DropdownModule } from 'primeng/dropdown';
import { TooltipModule } from 'primeng/tooltip';
import { FormsModule } from '@angular/forms';
import { currentUser, hasAnyRole } from '../../core/store/auth.store';
import { SystemRole } from '../../core/models/user.model';
import { AuthService } from '../../core/services/auth.service';
import { NotificationPanelComponent } from '../../shared/components/notification-panel/notification-panel.component';

/** Option de tenant pour le switcher SUPER_ADMIN. */
interface TenantOption {
  label: string;
  value: string;
}

/**
 * Barre superieure de l'application CliniTrak.
 *
 * <p>Contient :
 * <ul>
 *   <li>Titre de la page</li>
 *   <li>Panneau de notifications SSE avec badge non-lu</li>
 *   <li>TenantSwitcher (visible uniquement pour SUPER_ADMIN)</li>
 *   <li>Menu utilisateur (nom, role, deconnexion)</li>
 * </ul>
 */
@Component({
  selector: 'app-topbar',
  standalone: true,
  imports: [
    CommonModule,
    RouterLink,
    ButtonModule,
    DropdownModule,
    TooltipModule,
    FormsModule,
    NotificationPanelComponent,
  ],
  template: `
    <header class="tw-flex tw-items-center tw-justify-between tw-h-16 tw-px-6
                   tw-bg-white tw-border-b tw-border-gray-200 tw-shadow-sm tw-relative tw-z-40">

      <!-- Titre de la page -->
      <div class="tw-flex tw-items-center tw-gap-2">
        <h1 class="tw-text-lg tw-font-semibold tw-text-gray-800">CliniTrak</h1>
        <span class="tw-text-gray-400">/</span>
        <span class="tw-text-sm tw-text-gray-500">Tableau de bord</span>
      </div>

      <!-- Actions droite -->
      <div class="tw-flex tw-items-center tw-gap-2">

        <!-- TenantSwitcher — visible uniquement pour SUPER_ADMIN -->
        @if (isSuperAdmin()) {
          <div class="tw-flex tw-items-center tw-gap-2 tw-mr-2">
            <i class="pi pi-building tw-text-gray-400 tw-text-sm"></i>
            <p-dropdown
              [options]="availableTenants()"
              [(ngModel)]="selectedTenant"
              optionLabel="label"
              optionValue="value"
              placeholder="Changer de tenant"
              styleClass="tw-text-sm !tw-h-8 !tw-min-w-[160px]"
              (onChange)="onTenantChange($event.value)"
              [pTooltip]="'Basculer vers un autre tenant'"
              tooltipPosition="bottom"
            />
          </div>
        }

        <!-- Panneau de notifications -->
        <app-notification-panel />

        <!-- Separateur -->
        <div class="tw-w-px tw-h-6 tw-bg-gray-200 tw-mx-1"></div>

        <!-- Menu utilisateur -->
        <div class="tw-flex tw-items-center tw-gap-2">
          <!-- Avatar avec initiales -->
          <div class="tw-w-8 tw-h-8 tw-rounded-full tw-bg-blue-500
                      tw-flex tw-items-center tw-justify-center
                      tw-text-white tw-text-sm tw-font-bold tw-flex-shrink-0">
            {{ initials() }}
          </div>

          <!-- Nom et role (masques sur mobile) -->
          <div class="tw-hidden md:tw-block">
            <p class="tw-text-sm tw-font-medium tw-text-gray-700 tw-leading-tight">
              {{ currentUser()?.displayName }}
            </p>
            <p class="tw-text-xs tw-text-gray-500 tw-leading-tight">{{ firstRole() }}</p>
          </div>

          <!-- Bouton deconnexion -->
          <button
            (click)="onLogout()"
            class="tw-ml-1 tw-p-2 tw-text-gray-500 hover:tw-text-red-600
                   hover:tw-bg-red-50 tw-rounded-lg tw-transition-colors"
            title="Deconnexion"
          >
            <i class="pi pi-sign-out tw-text-lg"></i>
          </button>
        </div>
      </div>
    </header>
  `,
})
export class TopbarComponent {

  private readonly authService = inject(AuthService);

  protected readonly currentUser = currentUser;
  protected readonly isSuperAdmin = hasAnyRole(SystemRole.SUPER_ADMIN);

  /** Tenant actuellement selectionne dans le switcher. */
  protected selectedTenant: string = '';

  /** Liste simulee des tenants disponibles — a remplacer par un appel API. */
  protected readonly availableTenants = computed<TenantOption[]>(() => {
    if (!this.isSuperAdmin()) return [];
    return [
      { label: 'Saint-Luc', value: 'saintluc' },
      { label: 'CHU Liege', value: 'chu-liege' },
      { label: 'UZ Gent', value: 'uz-gent' },
      { label: 'Demo', value: 'demo' },
    ];
  });

  /** Indicateur de chargement du switcher de tenant. */
  protected readonly isSwitchingTenant = signal(false);

  /** Initiales de l'utilisateur pour l'avatar. */
  protected initials(): string {
    const user = currentUser();
    if (!user) return '?';
    const first = user.firstName?.charAt(0) ?? '';
    const last = user.lastName?.charAt(0) ?? '';
    return `${first}${last}`.toUpperCase() || user.email.charAt(0).toUpperCase();
  }

  /** Role principal de l'utilisateur (affiche sous le nom). */
  protected firstRole(): string {
    const roles = currentUser()?.roles ?? [];
    if (roles.length === 0) return '';
    return roles[0].replace('ROLE_', '').replace(/_/g, ' ');
  }

  /** Declenche la deconnexion. */
  protected onLogout(): void {
    this.authService.logout();
  }

  /**
   * Gere le changement de tenant par le SUPER_ADMIN.
   *
   * <p>Met a jour le header X-Tenant-ID via le store ou le localStorage.
   * Recharge la page pour appliquer le nouveau contexte.
   *
   * @param tenantId Identifiant du tenant selectionne
   */
  protected onTenantChange(tenantId: string): void {
    if (!tenantId) return;
    this.isSwitchingTenant.set(true);
    // Stocke le tenant override dans localStorage — l'intercepteur le lira
    localStorage.setItem('ct_tenant_override', tenantId);
    // Recharge la page pour reinitialiser le contexte complet
    window.location.reload();
  }
}
