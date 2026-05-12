import { Component } from '@angular/core';
import { RouterOutlet } from '@angular/router';
import { ToastModule } from 'primeng/toast';
import { ConfirmDialogModule } from 'primeng/confirmdialog';
import { MessageService, ConfirmationService } from 'primeng/api';
import { SidebarComponent } from '../sidebar/sidebar.component';
import { TopbarComponent } from '../topbar/topbar.component';
import { OfflineBannerComponent } from '../../shared/components/offline-banner/offline-banner.component';

/**
 * Layout principal de CliniTrak.
 *
 * <p>Structure :
 * <pre>
 * +------------------+------------------------------------+
 * | Sidebar          | Topbar                             |
 * | (navigation      +------------------------------------+
 * |  par module)     | &lt;router-outlet&gt;                    |
 * |                  | (contenu de la page courante)      |
 * +------------------+------------------------------------+
 * </pre>
 *
 * <p>Ce composant fournit egalement les services globaux PrimeNG :
 * <ul>
 *   <li>{@code p-toast} — notifications toast (via {@link MessageService})</li>
 *   <li>{@code p-confirmDialog} — boites de dialogue de confirmation</li>
 *   <li>{@link OfflineBannerComponent} — banniere de perte de connexion</li>
 * </ul>
 */
@Component({
  selector: 'app-main-layout',
  standalone: true,
  imports: [
    RouterOutlet,
    SidebarComponent,
    TopbarComponent,
    OfflineBannerComponent,
    ToastModule,
    ConfirmDialogModule,
  ],
  providers: [
    MessageService,
    ConfirmationService,
  ],
  template: `
    <!-- Banniere de perte de connexion (position fixed, hors du flux) -->
    <app-offline-banner />

    <!-- Toast global (messages de succes/erreur depuis tous les composants) -->
    <p-toast position="top-right" [breakpoints]="{ '768px': { width: '100%', right: '0', left: '0' } }" />

    <!-- Dialog de confirmation global -->
    <p-confirmDialog
      [style]="{ width: '28rem' }"
      acceptLabel="Confirmer"
      rejectLabel="Annuler"
      acceptButtonStyleClass="p-button-danger"
    />

    <!-- Mise en page principale -->
    <div class="tw-flex tw-h-screen tw-overflow-hidden tw-bg-gray-50">
      <!-- Sidebar fixe a gauche -->
      <app-sidebar class="tw-flex-shrink-0" />

      <!-- Zone principale droite -->
      <div class="tw-flex tw-flex-col tw-flex-1 tw-overflow-hidden">
        <!-- Topbar -->
        <app-topbar />

        <!-- Contenu de la page courante -->
        <main class="tw-flex-1 tw-overflow-y-auto tw-p-6">
          <router-outlet />
        </main>
      </div>
    </div>
  `,
})
export class MainLayoutComponent {}
