package be.clinitrak.notification.domain.entity;

import be.clinitrak.notification.domain.enums.NotificationType;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Entité représentant une notification envoyée à un utilisateur CliniTrak.
 * Supporte les notifications in-app (SSE) et email.
 */
@Entity
@Table(name = "notifications", indexes = {
        @Index(name = "idx_notifications_recipient", columnList = "recipient_user_id"),
        @Index(name = "idx_notifications_tenant", columnList = "tenant_id"),
        @Index(name = "idx_notifications_read", columnList = "is_read"),
        @Index(name = "idx_notifications_type", columnList = "type")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Notification extends BaseEntity {

    /**
     * Identifiant UUID de l'utilisateur destinataire de la notification.
     */
    @Column(name = "recipient_user_id", nullable = false)
    private UUID recipientUserId;

    /**
     * Identifiant du tenant auquel appartient le destinataire.
     */
    @Column(name = "tenant_id")
    private UUID tenantId;

    /**
     * Type fonctionnel de la notification.
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "type", nullable = false, length = 50)
    private NotificationType type;

    /**
     * Sujet de la notification (utilisé comme objet de l'email).
     */
    @Column(name = "subject")
    private String subject;

    /**
     * Corps textuel de la notification pour l'affichage in-app.
     */
    @Column(name = "message", columnDefinition = "TEXT")
    private String message;

    /**
     * Indique si la notification a été lue par le destinataire.
     */
    @Column(name = "is_read", nullable = false)
    @Builder.Default
    private boolean read = false;

    /**
     * Date et heure à laquelle la notification a été lue.
     */
    @Column(name = "read_at")
    private LocalDateTime readAt;

    /**
     * Indique si l'email a été envoyé avec succès.
     */
    @Column(name = "email_sent", nullable = false)
    @Builder.Default
    private boolean emailSent = false;

    /**
     * Date et heure d'envoi de l'email.
     */
    @Column(name = "email_sent_at")
    private LocalDateTime emailSentAt;

    /**
     * Nom du template Thymeleaf utilisé pour l'email (sans extension).
     */
    @Column(name = "template_name")
    private String templateName;
}
