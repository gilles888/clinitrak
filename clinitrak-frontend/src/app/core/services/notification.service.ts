import { inject, Injectable, signal } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '@env/environment';
import { Notification, NotificationType, Page } from '../models/notification.model';
import { currentUser } from '../store/auth.store';

/**
 * Service de gestion des notifications utilisateur.
 *
 * <p>Fournit :
 * <ul>
 *   <li>Connexion SSE pour les notifications temps-réel</li>
 *   <li>Chargement paginé des notifications depuis le backend</li>
 *   <li>Marquage lu individuel et global</li>
 *   <li>Signals réactifs pour les compteurs et la liste</li>
 * </ul>
 */
@Injectable({ providedIn: 'root' })
export class NotificationService {

  private readonly http = inject(HttpClient);
  private readonly API = `${environment.apiBaseUrl}/v1/notifications`;

  /** Signal : liste des notifications chargées (les plus récentes en tête). */
  readonly notifications = signal<Notification[]>([]);

  /** Signal : nombre de notifications non lues. */
  readonly unreadCount = signal<number>(0);

  /** Référence à l'EventSource SSE courant pour pouvoir le fermer. */
  private eventSource: EventSource | null = null;

  /**
   * Ouvre une connexion SSE vers le backend de notifications.
   *
   * <p>L'userId est transmis en query param car EventSource ne supporte pas
   * les headers personnalisés nativement.
   *
   * @returns L'instance EventSource créée, ou null si l'utilisateur n'est pas connecté
   */
  connectToStream(): EventSource | null {
    this.closeStream();

    const userId = currentUser()?.id;
    if (!userId) return null;
    const url = `${environment.apiBaseUrl}/v1/notifications/stream?userId=${encodeURIComponent(userId)}`;

    this.eventSource = new EventSource(url);

    this.eventSource.onmessage = (event: MessageEvent) => {
      try {
        const notif = JSON.parse(event.data) as Notification;
        this.notifications.update(list => [notif, ...list]);
        if (!notif.read) {
          this.unreadCount.update(n => n + 1);
        }
      } catch {
        // Ignore les messages malformés
      }
    };

    this.eventSource.onerror = () => {
      // Reconnexion automatique gérée par le navigateur (SSE standard)
    };

    return this.eventSource!;
  }

  /**
   * Ferme la connexion SSE si elle est ouverte.
   */
  closeStream(): void {
    if (this.eventSource) {
      this.eventSource.close();
      this.eventSource = null;
    }
  }

  /**
   * Charge les notifications de l'utilisateur courant (paginées).
   *
   * @param page Index de page (0-based)
   * @param size Nombre d'éléments par page
   * @returns Observable d'une page de notifications
   */
  getMyNotifications(page = 0, size = 20): Observable<Page<Notification>> {
    return this.http.get<Page<Notification>>(`${this.API}/my`, {
      params: { page: page.toString(), size: size.toString() },
    });
  }

  /**
   * Charge le nombre de notifications non lues.
   *
   * @returns Observable du compteur non-lu
   */
  getUnreadCount(): Observable<{ count: number }> {
    return this.http.get<{ count: number }>(`${this.API}/unread-count`);
  }

  /**
   * Marque une notification individuelle comme lue.
   *
   * @param id Identifiant UUID de la notification
   * @returns Observable vide (204)
   */
  markAsRead(id: string): Observable<void> {
    return this.http.patch<void>(`${this.API}/${id}/read`, {});
  }

  /**
   * Marque toutes les notifications de l'utilisateur comme lues.
   *
   * @returns Observable vide (204)
   */
  markAllAsRead(): Observable<void> {
    return this.http.patch<void>(`${this.API}/read-all`, {});
  }

  /**
   * Applique le marquage "lu" localement sur un item de la liste.
   *
   * @param id Identifiant de la notification à marquer
   */
  markReadLocally(id: string): void {
    this.notifications.update(list =>
      list.map(n => n.id === id ? { ...n, read: true } : n)
    );
    this.unreadCount.update(count => Math.max(0, count - 1));
  }

  /**
   * Applique le marquage "lu" sur toutes les notifications localement.
   */
  markAllReadLocally(): void {
    this.notifications.update(list => list.map(n => ({ ...n, read: true })));
    this.unreadCount.set(0);
  }

  /**
   * Retourne l'icône PrimeNG correspondant au type de notification.
   *
   * @param type Type de notification
   * @returns Classe CSS PrimeIcons
   */
  getIconForType(type: NotificationType): string {
    const icons: Record<NotificationType, string> = {
      [NotificationType.STOCK_ALERT]:       'pi pi-exclamation-triangle',
      [NotificationType.MEETING_REMINDER]:  'pi pi-calendar',
      [NotificationType.STUDY_UPDATE]:      'pi pi-file-edit',
      [NotificationType.ETHICS_DECISION]:   'pi pi-shield',
      [NotificationType.CTC_STATUS]:        'pi pi-sitemap',
      [NotificationType.DOCUMENT_UPLOADED]: 'pi pi-file',
      [NotificationType.USER_INVITATION]:   'pi pi-user-plus',
      [NotificationType.SYSTEM]:            'pi pi-info-circle',
      [NotificationType.EXPIRY_WARNING]:    'pi pi-clock',
    };
    return icons[type] ?? 'pi pi-bell';
  }

  /**
   * Retourne la classe de couleur Tailwind correspondant au type de notification.
   *
   * @param type Type de notification
   * @returns Classes CSS de couleur
   */
  getColorForType(type: NotificationType): string {
    const colors: Record<NotificationType, string> = {
      [NotificationType.STOCK_ALERT]:       'tw-text-red-500',
      [NotificationType.MEETING_REMINDER]:  'tw-text-yellow-500',
      [NotificationType.STUDY_UPDATE]:      'tw-text-blue-500',
      [NotificationType.ETHICS_DECISION]:   'tw-text-purple-500',
      [NotificationType.CTC_STATUS]:        'tw-text-teal-500',
      [NotificationType.DOCUMENT_UPLOADED]: 'tw-text-green-500',
      [NotificationType.USER_INVITATION]:   'tw-text-indigo-500',
      [NotificationType.SYSTEM]:            'tw-text-gray-500',
      [NotificationType.EXPIRY_WARNING]:    'tw-text-orange-500',
    };
    return colors[type] ?? 'tw-text-gray-400';
  }
}
