package be.clinitrak.ctc.dto;

import be.clinitrak.ctc.domain.enums.RegulatoryStatus;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

/**
 * DTO de réponse pour une étude sponsor CUSL.
 *
 * @param id                    identifiant UUID de l'entrée sponsor
 * @param studyId               identifiant de l'étude dans le study-service
 * @param projectManagerId      UUID du chef de projet
 * @param craIds                liste des UUID des CRA assignés
 * @param budgetTotal           budget total alloué
 * @param budgetSpent           montant déjà dépensé
 * @param milestones            jalons JSON libres
 * @param regulatoryStatus      statut réglementaire
 * @param regulatoryStatusLabel libellé français du statut réglementaire
 * @param createdAt             timestamp de création
 */
public record SponsorStudyResponse(
    UUID id,
    String studyId,
    String projectManagerId,
    List<String> craIds,
    BigDecimal budgetTotal,
    BigDecimal budgetSpent,
    String milestones,
    RegulatoryStatus regulatoryStatus,
    String regulatoryStatusLabel,
    LocalDateTime createdAt
) {}
