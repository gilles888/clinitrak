package be.clinitrak.notification.dto;

import be.clinitrak.notification.domain.enums.NotificationType;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.util.Map;
import java.util.UUID;

/**
 * Requête d'envoi d'une notification (email + in-app SSE).
 *
 * @param recipientEmail   adresse email du destinataire
 * @param recipientUserId  identifiant UUID de l'utilisateur destinataire
 * @param type             type fonctionnel de la notification
 * @param subject          sujet de la notification (objet de l'email)
 * @param templateName     nom du template email Thymeleaf (sans extension)
 * @param templateVars     variables à injecter dans le template
 * @param tenantId         identifiant du tenant du destinataire
 */
public record SendNotificationRequest(
        @NotBlank(message = "L'email du destinataire est obligatoire")
        @Email(message = "Format d'email invalide")
        String recipientEmail,

        @NotNull(message = "L'identifiant utilisateur est obligatoire")
        UUID recipientUserId,

        @NotNull(message = "Le type de notification est obligatoire")
        NotificationType type,

        @NotBlank(message = "Le sujet est obligatoire")
        String subject,

        String templateName,

        Map<String, Object> templateVars,

        UUID tenantId
) {}
