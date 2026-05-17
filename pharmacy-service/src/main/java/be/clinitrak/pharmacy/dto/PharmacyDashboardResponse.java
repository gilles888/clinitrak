package be.clinitrak.pharmacy.dto;

import java.util.List;

/**
 * Réponse du tableau de bord pharmacie avec les indicateurs clés.
 *
 * @param totalDrugs           nombre total de médicaments enregistrés
 * @param availableStocks      nombre de stocks en statut AVAILABLE
 * @param lowStockCount        nombre de stocks en dessous du seuil minimal
 * @param expiringIn30Days     nombre de stocks expirant dans les 30 prochains jours
 * @param pendingDispensations nombre total de dispensations enregistrées
 * @param openUnblindings      nombre de levées d'insu en attente d'approbation
 * @param urgentAlerts         liste des 5 alertes urgentes (EXPIRY_7 et LOW_STOCK)
 * @param dispensationsToday   nombre de dispensations effectuées aujourd'hui — widget frontend
 * @param criticalStock        nombre de lots AVAILABLE dont la quantité est en dessous du seuil critique (5 unités) — widget frontend
 */
public record PharmacyDashboardResponse(
    long totalDrugs,
    long availableStocks,
    long lowStockCount,
    long expiringIn30Days,
    long pendingDispensations,
    long openUnblindings,
    List<PharmacyAlert> urgentAlerts,
    long dispensationsToday,
    long criticalStock
) {}
