import { Component } from '@angular/core';
import { RouterLink } from '@angular/router';
import { ButtonModule } from 'primeng/button';

/**
 * Page d'acces refuse (403).
 *
 * <p>Affichee lorsqu'un utilisateur tente d'acceder
 * a une ressource pour laquelle il n'a pas les droits.
 */
@Component({
  selector: 'app-forbidden',
  standalone: true,
  imports: [RouterLink, ButtonModule],
  template: `
    <div class="tw-flex tw-flex-col tw-items-center tw-justify-center tw-min-h-screen tw-bg-gray-50 tw-text-center tw-px-4">
      <div class="tw-bg-red-100 tw-rounded-full tw-w-20 tw-h-20 tw-flex tw-items-center tw-justify-center tw-mb-6">
        <i class="pi pi-lock tw-text-4xl tw-text-red-500"></i>
      </div>
      <h1 class="tw-text-4xl tw-font-bold tw-text-gray-800 tw-mb-2">403</h1>
      <h2 class="tw-text-xl tw-font-semibold tw-text-gray-600 tw-mb-4">Acces refuse</h2>
      <p class="tw-text-sm tw-text-gray-500 tw-mb-8 tw-max-w-md">
        Vous n'avez pas les permissions necessaires pour acceder a cette page.
        Contactez votre administrateur si vous pensez qu'il s'agit d'une erreur.
      </p>
      <p-button
        label="Retour au tableau de bord"
        icon="pi pi-home"
        routerLink="/dashboard"
      />
    </div>
  `,
})
export class ForbiddenComponent {}
