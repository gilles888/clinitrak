package be.clinitrak.ctc.dto;

import be.clinitrak.ctc.domain.enums.AnalysisType;
import be.clinitrak.ctc.domain.enums.DataFormat;
import be.clinitrak.ctc.domain.enums.StatisticsStatus;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * DTO de réponse pour une demande d'analyse statistique.
 *
 * @param id                identifiant UUID de la demande
 * @param studyId           identifiant de l'étude associée
 * @param requestDate       date de soumission de la demande
 * @param requestorName     nom du demandeur
 * @param deadline          date limite de livraison
 * @param analysisType      type d'analyse
 * @param analysisTypeLabel libellé français du type d'analyse
 * @param dataFormat        format de sortie
 * @param dataFormatLabel   libellé français du format
 * @param status            statut courant
 * @param statusLabel       libellé français du statut
 * @param deliveredDate     date effective de livraison
 * @param createdAt         timestamp de création
 */
public record StatisticsRequestResponse(
    UUID id,
    String studyId,
    LocalDate requestDate,
    String requestorName,
    LocalDate deadline,
    AnalysisType analysisType,
    String analysisTypeLabel,
    DataFormat dataFormat,
    String dataFormatLabel,
    StatisticsStatus status,
    String statusLabel,
    LocalDate deliveredDate,
    LocalDateTime createdAt
) {}
