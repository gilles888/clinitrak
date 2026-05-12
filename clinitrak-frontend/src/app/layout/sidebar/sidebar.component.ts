import { Component, computed, signal } from '@angular/core';
import { RouterLink, RouterLinkActive } from '@angular/router';
import { NgClass } from '@angular/common';
import { currentUser, hasAnyRole } from '../../core/store/auth.store';
import { SystemRole } from '../../core/models/user.model';

/** Definition d'un item de navigation dans la sidebar. */
interface NavItem {
  label: string;
  /** Classe PrimeIcons (sans le prefixe 'pi'). */
  icon: string;
  route: string;
  /** Roles autorises — tableau vide = accessible a tous les utilisateurs connectes. */
  roles?: SystemRole[];
  badge?: string;
}

/**
 * Sidebar de navigation principale de CliniTrak.
 *
 * <p>Affiche uniquement les modules accessibles a l'utilisateur courant
 * (filtrage base sur les roles via les signals Angular).
 *
 * <p>Supporte un mode reduit (icones uniquement) bascule par l'utilisateur.
 */
@Component({
  selector: 'app-sidebar',
  standalone: true,
  imports: [RouterLink, RouterLinkActive, NgClass],
  template: `
    <aside
      class="tw-flex tw-flex-col tw-bg-slate-900 tw-text-white tw-shadow-xl tw-transition-all tw-duration-300"
      [class.tw-w-64]="!collapsed()"
      [class.tw-w-16]="collapsed()"
    >
      <!-- Logo -->
      <div class="tw-flex tw-items-center tw-justify-between tw-px-4 tw-py-5 tw-border-b tw-border-slate-700 tw-flex-shrink-0">
        @if (!collapsed()) {
          <div class="tw-flex tw-items-center tw-gap-2">
            <i class="pi pi-heart-fill tw-text-blue-400 tw-text-2xl"></i>
            <span class="tw-font-bold tw-text-lg tw-tracking-wide">CliniTrak</span>
          </div>
        }
        <button
          (click)="toggleCollapse()"
          class="tw-p-1.5 tw-rounded tw-text-slate-400 hover:tw-text-white hover:tw-bg-slate-700 tw-transition-colors tw-flex-shrink-0"
          [title]="collapsed() ? 'Agrandir la sidebar' : 'Reduire la sidebar'"
        >
          <i [class]="'pi ' + (collapsed() ? 'pi-chevron-right' : 'pi-chevron-left')"></i>
        </button>
      </div>

      <!-- Navigation principale -->
      <nav class="tw-flex-1 tw-py-4 tw-overflow-y-auto">
        <ul class="tw-space-y-0.5 tw-px-2">
          @for (item of visibleNavItems(); track item.route) {
            <li>
              <a
                [routerLink]="item.route"
                routerLinkActive="tw-bg-blue-600 !tw-text-white"
                [routerLinkActiveOptions]="{ exact: item.route === '/dashboard' }"
                class="tw-flex tw-items-center tw-gap-3 tw-px-3 tw-py-2.5 tw-rounded-lg
                       tw-text-slate-300 hover:tw-bg-slate-700 hover:tw-text-white
                       tw-transition-colors tw-group tw-no-underline"
                [title]="collapsed() ? item.label : ''"
              >
                <!-- Icone du module -->
                <i [class]="item.icon + ' tw-text-lg tw-flex-shrink-0'"></i>

                @if (!collapsed()) {
                  <span class="tw-text-sm tw-font-medium tw-flex-1 tw-truncate">{{ item.label }}</span>
                  @if (item.badge) {
                    <span class="tw-bg-blue-500 tw-text-white tw-text-xs tw-px-1.5 tw-py-0.5 tw-rounded-full tw-flex-shrink-0">
                      {{ item.badge }}
                    </span>
                  }
                }
              </a>
            </li>
          }
        </ul>
      </nav>

      <!-- Utilisateur connecte (pied de sidebar) -->
      <div class="tw-border-t tw-border-slate-700 tw-flex-shrink-0">
        @if (!collapsed()) {
          <div class="tw-px-4 tw-py-3">
            <div class="tw-flex tw-items-center tw-gap-2">
              <div class="tw-w-8 tw-h-8 tw-rounded-full tw-bg-blue-500 tw-flex tw-items-center tw-justify-center tw-text-sm tw-font-bold tw-flex-shrink-0">
                {{ userInitials() }}
              </div>
              <div class="tw-flex-1 tw-min-w-0">
                <p class="tw-text-sm tw-font-medium tw-text-white tw-truncate">{{ currentUser()?.displayName }}</p>
                <p class="tw-text-xs tw-text-slate-400 tw-truncate">{{ currentUser()?.email }}</p>
              </div>
            </div>
          </div>
        } @else {
          <div class="tw-px-2 tw-py-3 tw-flex tw-justify-center">
            <div class="tw-w-8 tw-h-8 tw-rounded-full tw-bg-blue-500 tw-flex tw-items-center tw-justify-center tw-text-sm tw-font-bold">
              {{ userInitials() }}
            </div>
          </div>
        }
      </div>
    </aside>
  `,
})
export class SidebarComponent {

  protected readonly currentUser = currentUser;
  protected readonly collapsed = signal(false);

  /** Tous les items de navigation avec leurs roles requis. */
  private readonly allNavItems: NavItem[] = [
    {
      label: 'Tableau de bord',
      icon: 'pi pi-home',
      route: '/dashboard',
    },
    {
      label: 'Etudes',
      icon: 'pi pi-file-edit',
      route: '/studies',
    },
    {
      label: 'Ethique (CE)',
      icon: 'pi pi-shield',
      route: '/ethics',
      roles: [
        SystemRole.CE_SECRETARY,
        SystemRole.CE_COORDINATOR,
        SystemRole.ADMIN_TENANT,
        SystemRole.SUPER_ADMIN,
      ],
    },
    {
      label: 'CTC',
      icon: 'pi pi-sitemap',
      route: '/ctc',
      roles: [
        SystemRole.CTC_DESK,
        SystemRole.CTC_CRA,
        SystemRole.CTC_PM,
        SystemRole.CTC_COFI,
        SystemRole.ADMIN_TENANT,
        SystemRole.SUPER_ADMIN,
      ],
    },
    {
      label: 'Pharmacie',
      icon: 'pi pi-database',
      route: '/pharmacy',
      roles: [
        SystemRole.PHARMACIST,
        SystemRole.ADMIN_TENANT,
        SystemRole.SUPER_ADMIN,
      ],
    },
    {
      label: 'Facturation',
      icon: 'pi pi-euro',
      route: '/billing',
      roles: [
        SystemRole.CTC_COFI,
        SystemRole.ADMIN_TENANT,
        SystemRole.SUPER_ADMIN,
      ],
    },
    {
      label: 'Echanges externes',
      icon: 'pi pi-globe',
      route: '/exchange',
      roles: [
        SystemRole.EXTERNAL,
        SystemRole.INVESTIGATOR,
        SystemRole.ADMIN_TENANT,
        SystemRole.SUPER_ADMIN,
      ],
    },
    {
      label: 'Documents',
      icon: 'pi pi-folder',
      route: '/documents',
    },
    {
      label: 'Notifications',
      icon: 'pi pi-bell',
      route: '/notifications',
    },
    {
      label: 'Administration',
      icon: 'pi pi-cog',
      route: '/admin',
      roles: [
        SystemRole.ADMIN_TENANT,
        SystemRole.SUPER_ADMIN,
      ],
    },
  ];

  /**
   * Items filtres selon les roles de l'utilisateur courant.
   *
   * <p>Un item sans roles definis est visible par tous les utilisateurs connectes.
   */
  protected readonly visibleNavItems = computed(() => {
    return this.allNavItems.filter(item => {
      if (!item.roles || item.roles.length === 0) return true;
      return hasAnyRole(...item.roles)();
    });
  });

  /** Initiales de l'utilisateur pour l'avatar. */
  protected readonly userInitials = computed(() => {
    const user = currentUser();
    if (!user) return '?';
    const first = user.firstName?.charAt(0) ?? '';
    const last = user.lastName?.charAt(0) ?? '';
    return `${first}${last}`.toUpperCase() || user.email.charAt(0).toUpperCase();
  });

  /** Bascule l'etat reduit/etendu de la sidebar. */
  protected toggleCollapse(): void {
    this.collapsed.update(v => !v);
  }
}
