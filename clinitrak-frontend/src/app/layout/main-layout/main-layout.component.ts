import { Component } from '@angular/core';
import { RouterOutlet } from '@angular/router';
import { SidebarComponent } from '../sidebar/sidebar.component';
import { TopbarComponent } from '../topbar/topbar.component';

/**
 * Layout principal de CliniTrak.
 *
 * <p>Structure :
 * <pre>
 * +------------------+------------------------------------+
 * | Sidebar          | Topbar                             |
 * | (navigation      +------------------------------------+
 * |  par module)     | <router-outlet>                    |
 * |                  | (contenu de la page courante)      |
 * +------------------+------------------------------------+
 * </pre>
 */
@Component({
  selector: 'app-main-layout',
  standalone: true,
  imports: [RouterOutlet, SidebarComponent, TopbarComponent],
  template: `
    <div class="tw-flex tw-h-screen tw-overflow-hidden tw-bg-gray-50">
      <!-- Sidebar fixe -->
      <app-sidebar class="tw-flex-shrink-0" />

      <!-- Zone principale -->
      <div class="tw-flex tw-flex-col tw-flex-1 tw-overflow-hidden">
        <!-- Topbar -->
        <app-topbar />

        <!-- Contenu de la page -->
        <main class="tw-flex-1 tw-overflow-y-auto tw-p-6">
          <router-outlet />
        </main>
      </div>
    </div>
  `,
})
export class MainLayoutComponent {}
