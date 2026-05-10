import { Component } from '@angular/core';
import { RouterOutlet } from '@angular/router';

/**
 * Composant racine de l'application CliniTrak.
 *
 * <p>Ce composant est minimal : il ne contient qu'un {@code <router-outlet>}.
 * Tout le layout (sidebar, topbar) est géré par {@link MainLayoutComponent}.
 */
@Component({
  selector: 'app-root',
  standalone: true,
  imports: [RouterOutlet],
  template: `<router-outlet />`,
})
export class AppComponent {}
