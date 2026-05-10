package be.clinitrak.ethics.dto;

import be.clinitrak.ethics.domain.entity.Meeting;

import java.util.List;
import java.util.Map;

/**
 * Réponse du tableau de bord du Comité d'Éthique.
 *
 * <p>Agrège les indicateurs clés pour la vue d'ensemble du secrétariat CE.
 *
 * @param pendingReviews          nombre d'avis en attente de décision
 * @param reviewsLastMonth        nombre d'avis soumis le mois dernier
 * @param upcomingMeetings        nombre de réunions dans les 30 prochains jours
 * @param nextMeeting             prochaine réunion planifiée (null si aucune)
 * @param annualReportsDue        nombre de rapports annuels dus dans les 60 prochains jours
 * @param annualReportsOverdue    nombre de rapports annuels en retard
 * @param reviewsByDecision       répartition des avis par décision (APPROVED: 42, ...)
 * @param recentPendingReviews    5 derniers avis en attente (pour la vue rapide)
 */
public record EthicsDashboardResponse(
    long pendingReviews,
    long reviewsLastMonth,
    long upcomingMeetings,
    Meeting nextMeeting,
    long annualReportsDue,
    long annualReportsOverdue,
    Map<String, Long> reviewsByDecision,
    List<EthicsReviewResponse> recentPendingReviews
) {}
