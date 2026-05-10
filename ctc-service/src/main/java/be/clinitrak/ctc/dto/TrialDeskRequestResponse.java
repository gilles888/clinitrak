package be.clinitrak.ctc.dto;

import be.clinitrak.ctc.domain.enums.DeskType;
import be.clinitrak.ctc.domain.enums.Priority;
import be.clinitrak.ctc.domain.enums.RequestStatus;
import be.clinitrak.ctc.domain.enums.RequestType;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * DTO de réponse pour une demande desk CTC.
 *
 * <p>Inclut tous les champs de la demande ainsi que les libellés français
 * des enums pour affichage dans l'interface utilisateur.
 *
 * @param id                    identifiant UUID de la demande
 * @param studyId               identifiant de l'étude associée
 * @param deskType              type de desk
 * @param deskTypeLabel         libellé français du type de desk
 * @param requestDate           date de soumission de la demande
 * @param requestorName         nom du demandeur
 * @param requestorEmail        email du demandeur
 * @param requestorOrganization organisation du demandeur
 * @param requestType           type de demande
 * @param requestTypeLabel      libellé français du type de demande
 * @param status                statut courant
 * @param statusLabel           libellé français du statut
 * @param assignedTo            UUID de l'assigné
 * @param priority              priorité
 * @param priorityLabel         libellé français de la priorité
 * @param deadline              date limite de traitement
 * @param notes                 notes libres
 * @param createdAt             timestamp de création
 */
public record TrialDeskRequestResponse(
    UUID id,
    String studyId,
    DeskType deskType,
    String deskTypeLabel,
    LocalDate requestDate,
    String requestorName,
    String requestorEmail,
    String requestorOrganization,
    RequestType requestType,
    String requestTypeLabel,
    RequestStatus status,
    String statusLabel,
    String assignedTo,
    Priority priority,
    String priorityLabel,
    LocalDate deadline,
    String notes,
    LocalDateTime createdAt
) {}
