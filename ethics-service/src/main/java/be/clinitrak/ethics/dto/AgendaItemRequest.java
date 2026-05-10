package be.clinitrak.ethics.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

import java.util.UUID;

/**
 * Requête d'ajout ou de modification d'un item de l'ordre du jour.
 *
 * @param studyId          identifiant de l'étude concernée (obligatoire)
 * @param itemOrder        ordre d'affichage dans l'ordre du jour (obligatoire)
 * @param itemType         type de point (ex: "ETUDE", "DIVERS", optionnel)
 * @param durationMinutes  durée estimée en minutes (optionnel)
 * @param comments         commentaires sur ce point (optionnel)
 */
public record AgendaItemRequest(
    @NotNull(message = "L'identifiant de l'étude est obligatoire")
    UUID studyId,

    @Min(value = 1, message = "L'ordre du point doit être supérieur à 0")
    int itemOrder,

    String itemType,
    int durationMinutes,
    String comments
) {}
