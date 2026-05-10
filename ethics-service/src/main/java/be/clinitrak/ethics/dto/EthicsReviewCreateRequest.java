package be.clinitrak.ethics.dto;

import be.clinitrak.ethics.domain.enums.ReviewType;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDate;
import java.util.UUID;

/**
 * Requête de création d'un avis éthique.
 *
 * @param studyId        identifiant de l'étude (obligatoire)
 * @param reviewType     type d'avis CE (obligatoire)
 * @param submissionDate date de soumission au secrétariat CE (obligatoire)
 * @param rapporteurName nom du rapporteur désigné (optionnel)
 * @param comments       commentaires initiaux (optionnel)
 */
public record EthicsReviewCreateRequest(
    @NotNull(message = "L'identifiant de l'étude est obligatoire")
    UUID studyId,

    @NotNull(message = "Le type d'avis est obligatoire")
    ReviewType reviewType,

    @NotNull(message = "La date de soumission est obligatoire")
    LocalDate submissionDate,

    String rapporteurName,
    String comments
) {}
