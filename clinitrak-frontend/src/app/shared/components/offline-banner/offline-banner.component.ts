import { Component, OnDestroy, OnInit, signal } from '@angular/core';
import { fromEvent, Subscription } from 'rxjs';

/**
 * Bannière d'alerte affichée en cas de perte de connexion réseau.
 *
 * <p>Utilise les événements natifs du navigateur {@code online} / {@code offline}
 * pour détecter l'état de la connectivité de manière réactive.
 *
 * <p>Se positionne en haut de la page en fixed pour rester visible quelle que
 * soit la position de scroll.
 */
@Component({
  selector: 'app-offline-banner',
  standalone: true,
  template: `
    @if (!isOnline()) {
      <div
        role="alert"
        aria-live="assertive"
        class="tw-fixed tw-top-0 tw-left-0 tw-right-0 tw-z-[9999]
               tw-bg-red-600 tw-text-white tw-text-center tw-py-2 tw-text-sm
               tw-flex tw-items-center tw-justify-center tw-gap-2 tw-shadow-lg"
      >
        <i class="pi pi-wifi tw-opacity-75"></i>
        <span>
          Connexion internet perdue — certaines fonctionnalités peuvent etre indisponibles
        </span>
      </div>
    }
  `,
})
export class OfflineBannerComponent implements OnInit, OnDestroy {

  /** Signal réactif reflétant l'état de connexion du navigateur. */
  protected readonly isOnline = signal(navigator.onLine);

  private readonly subscriptions = new Subscription();

  /** Abonne aux événements natifs online/offline. */
  ngOnInit(): void {
    this.subscriptions.add(
      fromEvent(window, 'online').subscribe(() => this.isOnline.set(true))
    );
    this.subscriptions.add(
      fromEvent(window, 'offline').subscribe(() => this.isOnline.set(false))
    );
  }

  /** Nettoie les abonnements aux événements. */
  ngOnDestroy(): void {
    this.subscriptions.unsubscribe();
  }
}
