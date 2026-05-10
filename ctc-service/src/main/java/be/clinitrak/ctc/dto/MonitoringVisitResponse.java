package be.clinitrak.ctc.dto;

import be.clinitrak.ctc.domain.enums.VisitStatus;
import be.clinitrak.ctc.domain.enums.VisitType;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * DTO de réponse pour une visite de monitoring.
 *
 * @param id                 identifiant UUID de la visite
 * @param studyId            identifiant de l'étude associée
 * @param visitDate          date de la visite
 * @param visitType          type de visite
 * @param visitTypeLabel     libellé français du type de visite
 * @param monitorName        nom du CRA
 * @param findings           constats documentés
 * @param correctionDeadline date limite de correction
 * @param status             statut courant
 * @param statusLabel        libellé français du statut
 * @param report             rapport de visite complet
 * @param createdAt          timestamp de création
 */
public record MonitoringVisitResponse(
    UUID id,
    String studyId,
    LocalDate visitDate,
    VisitType visitType,
    String visitTypeLabel,
    String monitorName,
    String findings,
    LocalDate correctionDeadline,
    VisitStatus status,
    String statusLabel,
    String report,
    LocalDateTime createdAt
) {}
