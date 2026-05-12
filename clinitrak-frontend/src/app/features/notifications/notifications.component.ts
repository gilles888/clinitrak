import { Component, inject, OnInit, signal } from '@angular/core';
import { CommonModule, DatePipe } from '@angular/common';
import { ButtonModule } from 'primeng/button';
import { CardModule } from 'primeng/card';
import { TagModule } from 'primeng/tag';
import { ProgressSpinnerModule } from 'primeng/progressspinner';
import { MessageService } from 'primeng/api';
import { catchError, throwError } from 'rxjs';
import { NotificationService } from '../../core/services/notification.service';
import { Notification, NotificationType } from '../../core/models/notification.model';

/**
 * Page complete des notifications de l'utilisateur.
 *
 * <p>Affiche toutes les notifications avec pagination,
 * filtrage par type et action de marquer comme lu.
 */
@Component({
  selector: 'app-notifications',
  standalone: true,
  imports: [
    CommonModule,
    DatePipe,
    ButtonModule,
    CardModule,
    TagModule,
    ProgressSpinnerModule,
  ],
  providers: [MessageService],
  template: `
    <div class="tw-space-y-6">

      <!-- En-tete -->
      <div class="tw-flex tw-items-center tw-justify-between">
        <div>
          <h1 class="tw-text-2xl tw-font-bold tw-text-gray-800">Notifications</h1>
          <p class="tw-text-sm tw-text-gray-500 tw-mt-1">
            Toutes vos notifications et alertes systeme
          </p>
        </div>
        @if (notificationService.unreadCount() > 0) {
          <p-button
            label="Tout marquer comme lu"
            icon="pi pi-check-circle"
            [outlined]="true"
            severity="secondary"
            size="small"
            (click)="onMarkAllAsRead()"
          />
        }
      </div>

      <!-- Indicateur de chargement -->
      @if (isLoading()) {
        <div class="tw-flex tw-justify-center tw-py-16">
          <p-progressSpinner styleClass="tw-w-12 tw-h-12" />
        </div>
      }

      <!-- Liste des notifications -->
      @if (!isLoading()) {
        @if (notificationService.notifications().length === 0) {
          <p-card styleClass="!tw-shadow-sm">
            <div class="tw-text-center tw-py-16 tw-text-gray-400">
              <i class="pi pi-bell-slash tw-text-5xl tw-mb-4 tw-block"></i>
              <p class="tw-text-base tw-font-medium">Aucune notification</p>
              <p class="tw-text-sm tw-mt-1">Vous etes a jour !</p>
            </div>
          </p-card>
        } @else {
          <div class="tw-space-y-2">
            @for (notif of notificationService.notifications(); track notif.id) {
              <div
                class="tw-bg-white tw-rounded-lg tw-border tw-border-gray-100 tw-shadow-sm
                       tw-flex tw-items-start tw-gap-4 tw-p-4 tw-cursor-pointer tw-transition-colors
                       hover:tw-bg-gray-50"
                [class.tw-border-l-4]="!notif.read"
                [class.tw-border-l-blue-500]="!notif.read"
                (click)="onMarkAsRead(notif)"
              >
                <!-- Icone coloree par type -->
                <div class="tw-flex-shrink-0 tw-w-10 tw-h-10 tw-rounded-full tw-flex tw-items-center tw-justify-center"
                     [class.tw-bg-red-100]="notif.type === NotificationType.STOCK_ALERT"
                     [class.tw-bg-yellow-100]="notif.type === NotificationType.MEETING_REMINDER"
                     [class.tw-bg-blue-100]="notif.type === NotificationType.STUDY_UPDATE"
                     [class.tw-bg-purple-100]="notif.type === NotificationType.ETHICS_DECISION"
                     [class.tw-bg-gray-100]="notif.type === NotificationType.SYSTEM">
                  <i [class]="getIcon(notif) + ' tw-text-lg'"></i>
                </div>

                <!-- Contenu -->
                <div class="tw-flex-1 tw-min-w-0">
                  <div class="tw-flex tw-items-start tw-justify-between tw-gap-2">
                    <p class="tw-text-sm tw-font-semibold tw-text-gray-800">{{ notif.subject }}</p>
                    <span class="tw-text-xs tw-text-gray-400 tw-flex-shrink-0">
                      {{ notif.createdAt | date:'dd/MM/yy HH:mm' }}
                    </span>
                  </div>
                  <p class="tw-text-sm tw-text-gray-600 tw-mt-0.5">{{ notif.message }}</p>
                  <div class="tw-flex tw-items-center tw-gap-2 tw-mt-2">
                    <p-tag
                      [value]="getTypeLabel(notif.type)"
                      [severity]="getTypeSeverity(notif.type)"
                      styleClass="tw-text-xs"
                    />
                    @if (!notif.read) {
                      <span class="tw-text-xs tw-text-blue-600 tw-font-medium">Non lu</span>
                    }
                  </div>
                </div>
              </div>
            }
          </div>

          <!-- Pagination manuelle -->
          @if (hasMore()) {
            <div class="tw-flex tw-justify-center tw-mt-4">
              <p-button
                label="Charger plus"
                icon="pi pi-chevron-down"
                [outlined]="true"
                severity="secondary"
                (click)="loadMore()"
                [loading]="isLoadingMore()"
              />
            </div>
          }
        }
      }
    </div>
  `,
})
export class NotificationsComponent implements OnInit {

  protected readonly notificationService = inject(NotificationService);
  private readonly messageService = inject(MessageService);

  protected readonly isLoading = signal(true);
  protected readonly isLoadingMore = signal(false);
  protected readonly hasMore = signal(false);

  protected readonly NotificationType = NotificationType;

  private currentPage = 0;

  ngOnInit(): void {
    this.loadNotifications(0);
  }

  /**
   * Retourne la classe d'icone avec couleur pour un type de notification.
   */
  protected getIcon(notif: Notification): string {
    return `${this.notificationService.getIconForType(notif.type)} ${this.notificationService.getColorForType(notif.type)}`;
  }

  /** Retourne le libelle d'un type de notification. */
  protected getTypeLabel(type: NotificationType): string {
    const labels: Record<NotificationType, string> = {
      [NotificationType.STOCK_ALERT]:       'Alerte stock',
      [NotificationType.MEETING_REMINDER]:  'Rappel reunion',
      [NotificationType.STUDY_UPDATE]:      'Mise a jour etude',
      [NotificationType.ETHICS_DECISION]:   'Decision CE',
      [NotificationType.CTC_STATUS]:        'Statut CTC',
      [NotificationType.DOCUMENT_UPLOADED]: 'Nouveau document',
      [NotificationType.USER_INVITATION]:   'Invitation',
      [NotificationType.SYSTEM]:            'Systeme',
      [NotificationType.EXPIRY_WARNING]:    'Expiration',
    };
    return labels[type] ?? type;
  }

  /** Retourne la severite PrimeNG pour un type de notification. */
  protected getTypeSeverity(type: NotificationType): 'success' | 'info' | 'warning' | 'danger' | 'secondary' {
    const severities: Record<NotificationType, 'success' | 'info' | 'warning' | 'danger' | 'secondary'> = {
      [NotificationType.STOCK_ALERT]:       'danger',
      [NotificationType.MEETING_REMINDER]:  'warning',
      [NotificationType.STUDY_UPDATE]:      'info',
      [NotificationType.ETHICS_DECISION]:   'info',
      [NotificationType.CTC_STATUS]:        'success',
      [NotificationType.DOCUMENT_UPLOADED]: 'success',
      [NotificationType.USER_INVITATION]:   'info',
      [NotificationType.SYSTEM]:            'secondary',
      [NotificationType.EXPIRY_WARNING]:    'warning',
    };
    return severities[type] ?? 'secondary';
  }

  /** Marque une notification comme lue. */
  protected onMarkAsRead(notif: Notification): void {
    if (notif.read) return;
    this.notificationService.markAsRead(notif.id).subscribe({
      next: () => this.notificationService.markReadLocally(notif.id),
      error: () => {},
    });
  }

  /** Marque toutes les notifications comme lues. */
  protected onMarkAllAsRead(): void {
    this.notificationService.markAllAsRead().pipe(
      catchError(err => {
        this.messageService.add({
          severity: 'error',
          summary: 'Erreur',
          detail: err.error?.detail ?? 'Impossible de marquer les notifications comme lues',
        });
        return throwError(() => err);
      })
    ).subscribe(() => {
      this.notificationService.markAllReadLocally();
    });
  }

  /** Charge la page suivante de notifications. */
  protected loadMore(): void {
    this.currentPage++;
    this.isLoadingMore.set(true);
    this.notificationService.getMyNotifications(this.currentPage).subscribe({
      next: page => {
        this.notificationService.notifications.update(list => [...list, ...page.content]);
        this.hasMore.set(!page.last);
        this.isLoadingMore.set(false);
      },
      error: () => {
        this.isLoadingMore.set(false);
      },
    });
  }

  private loadNotifications(page: number): void {
    this.isLoading.set(true);
    this.notificationService.getMyNotifications(page).subscribe({
      next: pageResult => {
        this.notificationService.notifications.set(pageResult.content);
        const unread = pageResult.content.filter(n => !n.read).length;
        this.notificationService.unreadCount.set(unread);
        this.hasMore.set(!pageResult.last);
        this.isLoading.set(false);
      },
      error: () => {
        this.isLoading.set(false);
      },
    });
  }
}
