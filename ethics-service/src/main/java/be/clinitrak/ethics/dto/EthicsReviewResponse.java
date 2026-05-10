package be.clinitrak.ethics.dto;

import be.clinitrak.ethics.domain.enums.ReviewDecision;
import be.clinitrak.ethics.domain.enums.ReviewType;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

/**
 * Réponse complète représentant un avis éthique.
 *
 * @param id              identifiant UUID de l'avis
 * @param tenantId        identifiant du tenant
 * @param studyId         identifiant de l'étude
 * @param ethicsNumber    numéro CE au format AAAA/NNNN
 * @param reviewType      type d'avis
 * @param reviewTypeLabel libellé français du type
 * @param submissionDate  date de soumission
 * @param reviewDate      date d'examen en réunion
 * @param decision        décision rendue
 * @param decisionLabel   libellé français de la décision
 * @param decisionDate    date officielle de la décision
 * @param comments        commentaires ou conditions de la décision
 * @param nextReviewDate  date du prochain examen obligatoire
 * @param rapporteurName  nom du rapporteur
 * @param reminderSent    indique si un rappel a été envoyé
 * @param createdAt       timestamp de création
 * @param updatedAt       timestamp de dernière modification
 */
public record EthicsReviewResponse(
    UUID id,
    UUID tenantId,
    UUID studyId,
    String ethicsNumber,
    ReviewType reviewType,
    String reviewTypeLabel,
    LocalDate submissionDate,
    LocalDate reviewDate,
    ReviewDecision decision,
    String decisionLabel,
    LocalDate decisionDate,
    String comments,
    LocalDate nextReviewDate,
    String rapporteurName,
    boolean reminderSent,
    Instant createdAt,
    Instant updatedAt
) {}
