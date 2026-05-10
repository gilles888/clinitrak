package be.clinitrak.ctc.dto;

import be.clinitrak.ctc.domain.enums.EventType;
import be.clinitrak.ctc.domain.enums.Severity;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDate;

/**
 * DTO de création d'un événement qualité.
 *
 * @param studyId     identifiant UUID de l'étude (obligatoire)
 * @param eventType   type d'événement qualité (obligatoire)
 * @param eventDate   date de l'événement (obligatoire)
 * @param severity    sévérité de l'événement (obligatoire)
 * @param description description détaillée de l'événement (obligatoire)
 */
public record QualityEventCreateRequest(
    @NotBlank String studyId,
    @NotNull EventType eventType,
    @NotNull LocalDate eventDate,
    @NotNull Severity severity,
    @NotBlank String description
) {}
