import { Component } from '@angular/core';
import { CardModule } from 'primeng/card';

/**
 * Composant placeholder du module Facturation.
 *
 * <p>Ce module sera implemente dans une session ulterieure.
 * Il gerera la facturation des etudes cliniques (CTC/COFI).
 */
@Component({
  selector: 'app-billing',
  standalone: true,
  imports: [CardModule],
  template: `
    <div class="tw-flex tw-flex-col tw-items-center tw-justify-center tw-min-h-64 tw-text-center">
      <i class="pi pi-euro tw-text-5xl tw-text-gray-300 tw-mb-4"></i>
      <h2 class="tw-text-xl tw-font-semibold tw-text-gray-600">Module Facturation</h2>
      <p class="tw-text-sm tw-text-gray-400 tw-mt-2 tw-max-w-md">
        Ce module est en cours de developpement.
        Il permettra la gestion de la facturation des etudes cliniques.
      </p>
    </div>
  `,
})
export class BillingComponent {}
