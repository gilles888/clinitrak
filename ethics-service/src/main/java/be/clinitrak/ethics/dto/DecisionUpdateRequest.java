package be.clinitrak.ethics.dto;

import be.clinitrak.ethics.domain.enums.ReviewDecision;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDate;

/**
 * Requête de mise à jour de la décision d'un avis éthique.
 *
 * @param decision       nouvelle décision (obligatoire, ne peut pas être PENDING)
 * @param decisionDate   date officielle de la décision (obligatoire)
 * @param reviewDate     date d'examen en réunion (optionnel)
 * @param nextReviewDate date du prochain examen obligatoire (optionnel)
 * @param comments       commentaires ou conditions de la décision (optionnel)
 */
public record DecisionUpdateRequest(
    @NotNull(message = "La décision est obligatoire")
    ReviewDecision decision,

    @NotNull(message = "La date de décision est obligatoire")
    LocalDate decisionDate,

    LocalDate reviewDate,
    LocalDate nextReviewDate,
    String comments
) {}
