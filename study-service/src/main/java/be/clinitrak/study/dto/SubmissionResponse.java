package be.clinitrak.study.dto;

import be.clinitrak.study.domain.enums.SubmissionStatus;
import be.clinitrak.study.domain.enums.SubmissionType;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

/**
 * DTO de réponse pour une soumission réglementaire.
 *
 * @param id              identifiant UUID de la soumission
 * @param submissionType  type de soumission
 * @param submissionDate  date de soumission effective
 * @param dueDate         date limite réglementaire
 * @param status          statut actuel de la soumission
 * @param submittedBy     identifiant de la personne ayant soumis
 * @param receivedDate    date de réception par les autorités
 * @param referenceNumber numéro de référence des autorités
 * @param comments        commentaires libres
 * @param createdAt       timestamp de création dans le système
 */
public record SubmissionResponse(
    UUID id,
    SubmissionType submissionType,
    LocalDate submissionDate,
    LocalDate dueDate,
    SubmissionStatus status,
    String submittedBy,
    LocalDate receivedDate,
    String referenceNumber,
    String comments,
    Instant createdAt
) {}
