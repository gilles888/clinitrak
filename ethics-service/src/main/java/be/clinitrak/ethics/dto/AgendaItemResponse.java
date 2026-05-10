package be.clinitrak.ethics.dto;

import be.clinitrak.ethics.domain.enums.ReviewDecision;

import java.util.UUID;

/**
 * Réponse représentant un item de l'ordre du jour d'une réunion CE.
 *
 * @param id               identifiant UUID de l'item
 * @param studyId          identifiant de l'étude concernée
 * @param itemOrder        ordre d'affichage
 * @param itemType         type de point (ex: "ETUDE", "DIVERS")
 * @param durationMinutes  durée estimée en minutes
 * @param decision         décision prise lors de la réunion (nullable avant la réunion)
 * @param decisionLabel    libellé français de la décision
 * @param comments         commentaires sur ce point
 */
public record AgendaItemResponse(
    UUID id,
    UUID studyId,
    int itemOrder,
    String itemType,
    int durationMinutes,
    ReviewDecision decision,
    String decisionLabel,
    String comments
) {}
