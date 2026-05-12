package be.clinitrak.notification.dto;

/**
 * Événement SSE (Server-Sent Event) transmis au frontend en temps réel.
 *
 * @param id      identifiant unique de l'événement
 * @param type    type de l'événement (correspond à {@code NotificationType.name()})
 * @param message message textuel de l'événement
 */
public record SseEvent(
        String id,
        String type,
        String message
) {}
