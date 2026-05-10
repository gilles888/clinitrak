package be.clinitrak.ctc.dto;

import be.clinitrak.ctc.domain.enums.Priority;
import jakarta.validation.constraints.NotBlank;

import java.time.LocalDate;

/**
 * DTO d'assignation d'une demande desk à un responsable CTC.
 *
 * <p>Permet au manager CTC de désigner un responsable, définir la priorité
 * et fixer une date limite de traitement.
 *
 * @param assignedTo UUID de l'utilisateur assigné (obligatoire)
 * @param priority   priorité de traitement
 * @param deadline   date limite imposée par le manager
 */
public record AssignRequest(
    @NotBlank String assignedTo,
    Priority priority,
    LocalDate deadline
) {}
