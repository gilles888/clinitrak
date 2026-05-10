package be.clinitrak.ctc.dto;

import be.clinitrak.ctc.domain.enums.AnalysisType;
import be.clinitrak.ctc.domain.enums.DataFormat;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDate;

/**
 * DTO de création d'une demande d'analyse statistique.
 *
 * @param studyId       identifiant UUID de l'étude (obligatoire)
 * @param requestorName nom du chercheur demandeur (obligatoire)
 * @param deadline      date limite de livraison des résultats (obligatoire)
 * @param analysisType  type d'analyse demandée (obligatoire)
 * @param dataFormat    format de sortie souhaité (obligatoire)
 */
public record StatisticsRequestCreateRequest(
    @NotBlank String studyId,
    @NotBlank String requestorName,
    @NotNull LocalDate deadline,
    @NotNull AnalysisType analysisType,
    @NotNull DataFormat dataFormat
) {}
