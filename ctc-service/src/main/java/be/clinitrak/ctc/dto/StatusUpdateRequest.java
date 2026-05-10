package be.clinitrak.ctc.dto;

import be.clinitrak.ctc.domain.enums.RequestStatus;
import jakarta.validation.constraints.NotNull;

/**
 * DTO de mise à jour du statut d'une demande desk CTC.
 *
 * @param status nouveau statut à appliquer (obligatoire)
 * @param notes  notes ou commentaires justifiant le changement de statut
 */
public record StatusUpdateRequest(
    @NotNull RequestStatus status,
    String notes
) {}
