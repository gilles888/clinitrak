package be.clinitrak.ctc.dto;

import java.util.List;

/**
 * DTO de réponse pour le tableau de bord du Centre de Thérapie Cellulaire.
 *
 * <p>Agrège les indicateurs clés de toutes les entités CTC
 * pour offrir une vue d'ensemble rapide au manager CTC.
 *
 * @param totalDeskRequests  nombre total de demandes desk actives
 * @param pendingRequests    nombre de demandes en attente de traitement
 * @param plannedVisits      nombre de visites de monitoring planifiées
 * @param openQualityEvents  nombre d'événements qualité ouverts
 * @param criticalEvents     nombre d'événements qualité de sévérité critique
 * @param activeContracts    nombre de contrats financiers actifs
 * @param pendingStatistics  nombre de demandes statistiques en attente
 * @param sponsorStudies     nombre d'études gérées en tant que sponsor CUSL
 * @param recentRequests     5 dernières demandes desk soumises
 */
public record CTCDashboardResponse(
    long totalDeskRequests,
    long pendingRequests,
    long plannedVisits,
    long openQualityEvents,
    long criticalEvents,
    long activeContracts,
    long pendingStatistics,
    long sponsorStudies,
    List<TrialDeskRequestResponse> recentRequests
) {}
