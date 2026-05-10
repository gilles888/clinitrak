package be.clinitrak.ethics.dto;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

/**
 * Réponse représentant une correspondance générée par le Comité d'Éthique.
 *
 * @param id             identifiant UUID de la correspondance
 * @param studyId        identifiant de l'étude
 * @param reviewId       identifiant de l'avis CE associé (nullable)
 * @param subject        sujet de la correspondance
 * @param recipientEmail email du destinataire
 * @param recipientName  nom du destinataire
 * @param generatedDate  date de génération
 * @param sentDate       date d'envoi (null si non encore envoyé)
 * @param sent           indique si la correspondance a été envoyée
 * @param sendError      message d'erreur d'envoi (null si succès)
 * @param createdAt      timestamp de création
 */
public record CorrespondenceResponse(
    UUID id,
    UUID studyId,
    UUID reviewId,
    String subject,
    String recipientEmail,
    String recipientName,
    LocalDate generatedDate,
    LocalDate sentDate,
    boolean sent,
    String sendError,
    Instant createdAt
) {}
