package be.clinitrak.study.dto;

import be.clinitrak.study.domain.enums.StudyStatus;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDate;

/**
 * DTO pour la mise à jour du statut d'une étude clinique.
 *
 * <p>Chaque changement de statut crée une entrée dans l'historique ({@code StudyStatusHistory}).
 *
 * @param status     nouveau statut de l'étude (obligatoire)
 * @param statusDate date effective du changement de statut (obligatoire)
 * @param comment    commentaire ou justification du changement
 */
public record StatusUpdateRequest(
    @NotNull(message = "Le nouveau statut est obligatoire")
    StudyStatus status,

    @NotNull(message = "La date du changement de statut est obligatoire")
    LocalDate statusDate,

    String comment
) {}
