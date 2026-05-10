package be.clinitrak.ctc.dto;

import jakarta.validation.constraints.NotBlank;

import java.math.BigDecimal;

/**
 * DTO de création d'une étude sponsor CUSL.
 *
 * @param studyId          identifiant UUID de l'étude dans le study-service (obligatoire, unique)
 * @param projectManagerId UUID de l'utilisateur chef de projet
 * @param budgetTotal      budget total alloué à l'étude
 */
public record SponsorStudyCreateRequest(
    @NotBlank String studyId,
    String projectManagerId,
    BigDecimal budgetTotal
) {}
