import { Component, inject, OnDestroy, OnInit, signal } from '@angular/core';
import { CommonModule, DatePipe } from '@angular/common';
import { RouterLink } from '@angular/router';
import { ButtonModule } from 'primeng/button';
import { OverlayPanelModule } from 'primeng/overlaypanel';
import { BadgeModule } from 'primeng/badge';
import { ProgressSpinnerModule } from 'primeng/progressspinner';
import { MessageService } from 'primeng/api';
import { catchError, throwError } from 'rxjs';
import { NotificationService } from '../../../core/services/notification.service';
import { Notification } from '../../../core/models/notification.model';

/**
 * Panneau de notifications accessible depuis la topbar.
 *
 * <p>Affiche les notifications de l'utilisateur dans un OverlayPanel PrimeNG.
 * Se connecte au flux SSE au démarrage et se déconnecte à la destruction.
 */
@Component({
  selector: 'app-notification-panel',
  standalone: true,
  imports: [
    CommonModule,
    RouterLink,
    ButtonModule,
    OverlayPanelModule,
    BadgeModule,
    ProgressSpinnerModule,
    DatePipe,
  ],
  template: `
    <!-- Bouton cloche avec badge -->
    <div class="tw-relative">
      <button
        (click)="panel.toggle($event)"
        class="tw-relative tw-p-2 tw-text-gray-500 hover:tw-text-gray-700
               hover:tw-bg-gray-100 tw-rounded-lg tw-transition-colors tw-flex tw-items-center tw-justify-center"
        title="Notifications"
      >
        <i class="pi pi-bell tw-text-lg"></i>
        @if (notificationService.unreadCount() > 0) {
          <span class="tw-absolute tw-top-0.5 tw-right-0.5 tw-min-w-[18px] tw-h-[18px]
                       tw-bg-red-500 tw-text-white tw-text-xs tw-font-bold
                       tw-rounded-full tw-flex tw-items-center tw-justify-center tw-px-1">
            {{ notificationService.unreadCount() > 99 ? '99+' : notificationService.unreadCount() }}
          </span>
        }
      </button>
    </div>

    <!-- OverlayPanel des notifications -->
    <p-overlayPanel #panel styleClass="tw-w-96 !tw-p-0">
      <!-- En-tête -->
      <div class="tw-flex tw-justify-between tw-items-center tw-px-4 tw-py-3 tw-border-b tw-border-gray-100">
        <h3 class="tw-font-semibold tw-text-gray-800 tw-text-base">Notifications</h3>
        @if (notificationService.unreadCount() > 0) {
          <button
            (click)="onMarkAllAsRead()"
            class="tw-text-xs tw-text-blue-600 hover:tw-text-blue-800 tw-font-medium tw-transition-colors"
          >
            Tout marquer comme lu
          </button>
        }
      </div>

      <!-- Liste des notifications -->
      <div class="tw-max-h-96 tw-overflow-y-auto">
        @if (isLoading()) {
          <div class="tw-flex tw-justify-center tw-py-8">
            <p-progressSpinner styleClass="tw-w-8 tw-h-8" />
          </div>
        } @else if (notificationService.notifications().length === 0) {
          <div class="tw-text-center tw-py-10 tw-px-4">
            <i class="pi pi-bell-slash tw-text-3xl tw-text-gray-300 tw-mb-2 tw-block"></i>
            <p class="tw-text-sm tw-text-gray-400">Aucune notification</p>
          </div>
        } @else {
          @for (notif of notificationService.notifications(); track notif.id) {
            <div
              class="tw-flex tw-items-start tw-gap-3 tw-px-4 tw-py-3 tw-cursor-pointer
                     tw-border-b tw-border-gray-50 tw-transition-colors
                     hover:tw-bg-gray-50"
              [class.tw-bg-blue-50]="!notif.read"
              (click)="onMarkAsRead(notif)"
            >
              <!-- Icône colorée selon le type -->
              <div class="tw-flex-shrink-0 tw-mt-0.5">
                <i [class]="getIcon(notif) + ' tw-text-base'"></i>
              </div>

              <!-- Contenu -->
              <div class="tw-flex-1 tw-min-w-0">
                <p class="tw-text-sm tw-font-medium tw-text-gray-800 tw-leading-snug">
                  {{ notif.subject }}
                </p>
                <p class="tw-text-xs tw-text-gray-500 tw-mt-0.5 tw-line-clamp-2">
                  {{ notif.message }}
                </p>
                <p class="tw-text-xs tw-text-gray-400 tw-mt-1">
                  {{ notif.createdAt | date:'dd/MM/yy HH:mm' }}
                </p>
              </div>

              <!-- Point non-lu -->
              @if (!notif.read) {
                <div class="tw-flex-shrink-0 tw-w-2 tw-h-2 tw-rounded-full tw-bg-blue-500 tw-mt-2"></div>
              }
            </div>
          }
        }
      </div>

      <!-- Pied de panneau -->
      <div class="tw-border-t tw-border-gray-100 tw-px-4 tw-py-2.5 tw-text-center">
        <a
          routerLink="/notifications"
          class="tw-text-sm tw-text-blue-600 hover:tw-text-blue-800 tw-font-medium tw-transition-colors"
          (click)="panel.hide()"
        >
          Voir toutes les notifications
        </a>
      </div>
    </p-overlayPanel>
  `,
})
export class NotificationPanelComponent implements OnInit, OnDestroy {

  protected readonly notificationService = inject(NotificationService);
  private readonly messageService = inject(MessageService);

  /** Indicateur de chargement initial des notifications. */
  protected readonly isLoading = signal(false);

  /** Charge les notifications initiales et ouvre le flux SSE si active. */
  ngOnInit(): void {
    this.loadNotifications();
    this.notificationService.connectToStream();
  }

  /** Ferme le flux SSE à la destruction du composant. */
  ngOnDestroy(): void {
    this.notificationService.closeStream();
  }

  /**
   * Retourne la classe d'icône avec couleur pour un type de notification.
   *
   * @param notif Notification à afficher
   * @returns Classes CSS combinées (icône + couleur)
   */
  protected getIcon(notif: Notification): string {
    return `${this.notificationService.getIconForType(notif.type)} ${this.notificationService.getColorForType(notif.type)}`;
  }

  /** Marque une notification individuelle comme lue. */
  protected onMarkAsRead(notif: Notification): void {
    if (notif.read) return;
    this.notificationService.markAsRead(notif.id).pipe(
      catchError(err => {
        this.messageService.add({
          severity: 'warn',
          summary: 'Attention',
          detail: 'Impossible de marquer la notification comme lue',
        });
        return throwError(() => err);
      })
    ).subscribe(() => {
      this.notificationService.markReadLocally(notif.id);
    });
  }

  /** Marque toutes les notifications comme lues. */
  protected onMarkAllAsRead(): void {
    this.notificationService.markAllAsRead().pipe(
      catchError(err => {
        this.messageService.add({
          severity: 'warn',
          summary: 'Attention',
          detail: 'Impossible de marquer toutes les notifications comme lues',
        });
        return throwError(() => err);
      })
    ).subscribe(() => {
      this.notificationService.markAllReadLocally();
    });
  }

  /** Charge la premiere page de notifications depuis le backend. */
  private loadNotifications(): void {
    this.isLoading.set(true);
    this.notificationService.getMyNotifications(0, 20).subscribe({
      next: page => {
        this.notificationService.notifications.set(page.content);
        const unread = page.content.filter(n => !n.read).length;
        this.notificationService.unreadCount.set(unread);
        this.isLoading.set(false);
      },
      error: () => {
        this.isLoading.set(false);
        // Silencieux si le service n'est pas disponible
      },
    });
  }
}
