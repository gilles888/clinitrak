import { Component, inject } from '@angular/core';
import { RouterLink } from '@angular/router';
import { currentUser } from '../../core/store/auth.store';
import { AuthService } from '../../core/services/auth.service';

/**
 * Barre supérieure de l'application.
 *
 * <p>Contient :
 * <ul>
 *   <li>Fil d'Ariane (breadcrumb, à connecter via ActivatedRoute)</li>
 *   <li>Notifications (badge)</li>
 *   <li>Menu utilisateur (profil, déconnexion)</li>
 * </ul>
 */
@Component({
  selector: 'app-topbar',
  standalone: true,
  imports: [RouterLink],
  template: `
    <header class="tw-flex tw-items-center tw-justify-between tw-h-16 tw-px-6
                   tw-bg-white tw-border-b tw-border-gray-200 tw-shadow-sm">

      <!-- Titre de la page (à connecter au router) -->
      <div class="tw-flex tw-items-center tw-gap-2">
        <h1 class="tw-text-lg tw-font-semibold tw-text-gray-800">CliniTrak</h1>
        <span class="tw-text-gray-400">/</span>
        <span class="tw-text-sm tw-text-gray-500">Tableau de bord</span>
      </div>

      <!-- Actions droite -->
      <div class="tw-flex tw-items-center tw-gap-3">

        <!-- Notification bell -->
        <button
          class="tw-relative tw-p-2 tw-text-gray-500 hover:tw-text-gray-700
                 hover:tw-bg-gray-100 tw-rounded-lg tw-transition-colors"
          title="Notifications"
        >
          <i class="pi pi-bell tw-text-lg"></i>
          <span class="tw-absolute tw-top-1.5 tw-right-1.5 tw-w-2 tw-h-2
                       tw-bg-red-500 tw-rounded-full"></span>
        </button>

        <!-- Séparateur -->
        <div class="tw-w-px tw-h-6 tw-bg-gray-200"></div>

        <!-- Menu utilisateur -->
        <div class="tw-flex tw-items-center tw-gap-2">
          <div class="tw-w-8 tw-h-8 tw-rounded-full tw-bg-blue-500
                      tw-flex tw-items-center tw-justify-center
                      tw-text-white tw-text-sm tw-font-bold">
            {{ initials() }}
          </div>
          <div class="tw-hidden md:tw-block">
            <p class="tw-text-sm tw-font-medium tw-text-gray-700">{{ currentUser()?.displayName }}</p>
            <p class="tw-text-xs tw-text-gray-500">{{ firstRole() }}</p>
          </div>
          <button
            (click)="onLogout()"
            class="tw-ml-2 tw-p-2 tw-text-gray-500 hover:tw-text-red-600
                   hover:tw-bg-red-50 tw-rounded-lg tw-transition-colors"
            title="Déconnexion"
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

  /** Initiales de l'utilisateur pour l'avatar. */
  protected initials(): string {
    const user = currentUser();
    if (!user) return '?';
    return `${user.firstName.charAt(0)}${user.lastName.charAt(0)}`.toUpperCase();
  }

  /** Rôle principal de l'utilisateur (affiché sous le nom). */
  protected firstRole(): string {
    const roles = currentUser()?.roles ?? [];
    if (roles.length === 0) return '';
    return roles[0].replace('ROLE_', '').replace(/_/g, ' ');
  }

  /** Déclenche la déconnexion. */
  protected onLogout(): void {
    this.authService.logout();
  }
}
