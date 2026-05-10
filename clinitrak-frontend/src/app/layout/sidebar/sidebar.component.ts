import { Component, computed, signal } from '@angular/core';
import { RouterLink, RouterLinkActive } from '@angular/router';
import { NgClass, NgFor, NgIf } from '@angular/common';
import { currentUser, hasAnyRole } from '../../core/store/auth.store';
import { SystemRole } from '../../core/models/user.model';

/** Définition d'un item de navigation dans la sidebar. */
interface NavItem {
  label: string;
  icon: string;
  route: string;
  roles?: SystemRole[];
  badge?: string;
}

/**
 * Sidebar de navigation principale de CliniTrak.
 *
 * <p>Affiche uniquement les modules accessibles à l'utilisateur courant
 * (filtrage basé sur les rôles via les signals Angular).
 */
@Component({
  selector: 'app-sidebar',
  standalone: true,
  imports: [RouterLink, RouterLinkActive, NgClass, NgFor, NgIf],
  template: `
    <aside
      class="tw-flex tw-flex-col tw-w-64 tw-min-h-screen tw-bg-slate-900 tw-text-white tw-shadow-xl"
      [class.tw-w-16]="collapsed()"
    >
      <!-- Logo -->
      <div class="tw-flex tw-items-center tw-justify-between tw-px-4 tw-py-5 tw-border-b tw-border-slate-700">
        @if (!collapsed()) {
          <div class="tw-flex tw-items-center tw-gap-2">
            <span class="tw-text-2xl">🏥</span>
            <span class="tw-font-bold tw-text-lg tw-tracking-wide">CliniTrak</span>
          </div>
        }
        <button
          (click)="toggleCollapse()"
          class="tw-p-1.5 tw-rounded tw-text-slate-400 hover:tw-text-white hover:tw-bg-slate-700 tw-transition-colors"
          [title]="collapsed() ? 'Agrandir' : 'Réduire'"
        >
          <i [class]="collapsed() ? 'pi pi-chevron-right' : 'pi pi-chevron-left'"></i>
        </button>
      </div>

      <!-- Navigation -->
      <nav class="tw-flex-1 tw-py-4 tw-overflow-y-auto">
        <ul class="tw-space-y-1 tw-px-2">
          @for (item of visibleNavItems(); track item.route) {
            <li>
              <a
                [routerLink]="item.route"
                routerLinkActive="tw-bg-blue-600 tw-text-white"
                [routerLinkActiveOptions]="{ exact: item.route === '/' }"
                class="tw-flex tw-items-center tw-gap-3 tw-px-3 tw-py-2.5 tw-rounded-lg tw-text-slate-300
                       hover:tw-bg-slate-700 hover:tw-text-white tw-transition-colors tw-group"
              >
                <i [class]="'pi ' + item.icon + ' tw-text-lg tw-flex-shrink-0'"></i>
                @if (!collapsed()) {
                  <span class="tw-text-sm tw-font-medium">{{ item.label }}</span>
                  @if (item.badge) {
                    <span class="tw-ml-auto tw-bg-blue-500 tw-text-white tw-text-xs tw-px-1.5 tw-py-0.5 tw-rounded-full">
                      {{ item.badge }}
                    </span>
                  }
                }
              </a>
            </li>
          }
        </ul>
      </nav>

      <!-- Utilisateur connecté -->
      @if (!collapsed()) {
        <div class="tw-px-4 tw-py-3 tw-border-t tw-border-slate-700">
          <div class="tw-flex tw-items-center tw-gap-2">
            <div class="tw-w-8 tw-h-8 tw-rounded-full tw-bg-blue-500 tw-flex tw-items-center tw-justify-center tw-text-sm tw-font-bold">
              {{ userInitials() }}
            </div>
            <div class="tw-flex-1 tw-min-w-0">
              <p class="tw-text-sm tw-font-medium tw-text-white tw-truncate">{{ currentUser()?.displayName }}</p>
              <p class="tw-text-xs tw-text-slate-400 tw-truncate">{{ currentUser()?.email }}</p>
            </div>
          </div>
        </div>
      }
    </aside>
  `,
})
export class SidebarComponent {

  protected readonly currentUser = currentUser;
  protected readonly collapsed = signal(false);

  /** Items de navigation avec leurs rôles requis. */
  private readonly allNavItems: NavItem[] = [
    { label: 'Tableau de bord', icon: 'pi-home',          route: '/dashboard' },
    { label: 'Études',          icon: 'pi-book',          route: '/studies' },
    { label: 'Éthique (CE)',    icon: 'pi-shield',        route: '/ethics',   roles: [SystemRole.CE_SECRETARY, SystemRole.CE_COORDINATOR, SystemRole.ADMIN_TENANT, SystemRole.SUPER_ADMIN] },
    { label: 'CTC',             icon: 'pi-sitemap',       route: '/ctc',      roles: [SystemRole.CTC_DESK, SystemRole.CTC_CRA, SystemRole.CTC_PM, SystemRole.CTC_COFI, SystemRole.ADMIN_TENANT, SystemRole.SUPER_ADMIN] },
    { label: 'Pharmacie',       icon: 'pi-heart',         route: '/pharmacy', roles: [SystemRole.PHARMACIST, SystemRole.ADMIN_TENANT, SystemRole.SUPER_ADMIN] },
    { label: 'Facturation',     icon: 'pi-euro',          route: '/billing',  roles: [SystemRole.CTC_COFI, SystemRole.ADMIN_TENANT, SystemRole.SUPER_ADMIN] },
    { label: 'Documents',       icon: 'pi-folder',        route: '/documents' },
    { label: 'Administration',  icon: 'pi-cog',           route: '/admin',    roles: [SystemRole.ADMIN_TENANT, SystemRole.SUPER_ADMIN] },
  ];

  /** Items filtrés selon les rôles de l'utilisateur courant. */
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
    return `${user.firstName.charAt(0)}${user.lastName.charAt(0)}`.toUpperCase();
  });

  /** Bascule l'état réduit/étendu de la sidebar. */
  protected toggleCollapse(): void {
    this.collapsed.update(v => !v);
  }
}
