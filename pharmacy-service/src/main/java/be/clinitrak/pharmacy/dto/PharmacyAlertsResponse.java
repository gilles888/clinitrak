package be.clinitrak.pharmacy.dto;

import java.util.List;

/**
 * Réponse contenant la liste des alertes pharmacie et le nombre d'alertes critiques.
 *
 * @param alerts        liste complète des alertes actives
 * @param criticalCount nombre d'alertes critiques (EXPIRY_7 et LOW_STOCK)
 */
public record PharmacyAlertsResponse(
    List<PharmacyAlert> alerts,
    int criticalCount
) {}
