package be.clinitrak.ctc.dto;

import be.clinitrak.ctc.domain.enums.VisitType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDate;

/**
 * DTO de création d'une visite de monitoring.
 *
 * @param studyId            identifiant UUID de l'étude (obligatoire)
 * @param visitDate          date de la visite (obligatoire)
 * @param visitType          type de visite (obligatoire)
 * @param monitorName        nom du CRA réalisant la visite (obligatoire)
 * @param correctionDeadline date limite pour corriger les écarts identifiés
 */
public record MonitoringVisitCreateRequest(
    @NotBlank String studyId,
    @NotNull LocalDate visitDate,
    @NotNull VisitType visitType,
    @NotBlank String monitorName,
    LocalDate correctionDeadline
) {}
