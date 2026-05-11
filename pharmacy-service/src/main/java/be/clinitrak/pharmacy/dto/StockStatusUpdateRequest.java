package be.clinitrak.pharmacy.dto;

import be.clinitrak.pharmacy.domain.enums.StockStatus;
import jakarta.validation.constraints.NotNull;

/**
 * Requête de mise à jour du statut d'un stock de médicament.
 *
 * @param status nouveau statut à appliquer (obligatoire)
 */
public record StockStatusUpdateRequest(
    @NotNull StockStatus status
) {}
