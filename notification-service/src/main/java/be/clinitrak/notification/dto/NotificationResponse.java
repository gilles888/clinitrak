package be.clinitrak.notification.dto;

import be.clinitrak.notification.domain.enums.NotificationType;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Réponse contenant les informations d'une notification.
 *
 * @param id        identifiant unique de la notification
 * @param type      type fonctionnel de la notification
 * @param subject   sujet de la notification
 * @param message   corps textuel de la notification
 * @param read      indique si la notification a été lue
 * @param readAt    date et heure de lecture (null si non lue)
 * @param createdAt date et heure de création
 */
public record NotificationResponse(
        UUID id,
        NotificationType type,
        String subject,
        String message,
        boolean read,
        LocalDateTime readAt,
        LocalDateTime createdAt
) {}
