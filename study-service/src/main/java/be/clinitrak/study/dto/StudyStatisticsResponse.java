package be.clinitrak.study.dto;

import java.util.Map;

/**
 * DTO de statistiques agrégées pour les études cliniques d'un tenant.
 *
 * <p>Retourné par GET /studies/statistics pour alimenter le tableau de bord.
 *
 * @param totalStudies       nombre total d'études (non supprimées)
 * @param draftStudies       nombre d'études en brouillon
 * @param ongoingStudies     nombre d'études en cours
 * @param approvedStudies    nombre d'études approuvées
 * @param closedStudies      nombre d'études clôturées
 * @param sponsorCuslStudies nombre d'études dont les CUSL sont promoteur
 * @param byTherapeuticArea  répartition par domaine thérapeutique (domaine → count)
 * @param byPhase            répartition par phase d'essai (phase → count)
 */
public record StudyStatisticsResponse(
    long totalStudies,
    long draftStudies,
    long ongoingStudies,
    long approvedStudies,
    long closedStudies,
    long sponsorCuslStudies,
    Map<String, Long> byTherapeuticArea,
    Map<String, Long> byPhase
) {}
