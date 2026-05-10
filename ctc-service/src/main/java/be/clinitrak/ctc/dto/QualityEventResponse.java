package be.clinitrak.ctc.dto;

import be.clinitrak.ctc.domain.enums.EventStatus;
import be.clinitrak.ctc.domain.enums.EventType;
import be.clinitrak.ctc.domain.enums.Severity;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * DTO de réponse pour un événement qualité.
 *
 * @param id                identifiant UUID de l'événement
 * @param studyId           identifiant de l'étude associée
 * @param eventType         type d'événement
 * @param eventTypeLabel    libellé français du type d'événement
 * @param eventDate         date de l'événement
 * @param severity          sévérité
 * @param severityLabel     libellé français de la sévérité
 * @param description       description détaillée
 * @param rootCause         analyse de la cause racine
 * @param correctiveAction  action corrective mise en place
 * @param status            statut courant
 * @param statusLabel       libellé français du statut
 * @param closureDate       date de clôture
 * @param createdAt         timestamp de création
 */
public record QualityEventResponse(
    UUID id,
    String studyId,
    EventType eventType,
    String eventTypeLabel,
    LocalDate eventDate,
    Severity severity,
    String severityLabel,
    String description,
    String rootCause,
    String correctiveAction,
    EventStatus status,
    String statusLabel,
    LocalDate closureDate,
    LocalDateTime createdAt
) {}
