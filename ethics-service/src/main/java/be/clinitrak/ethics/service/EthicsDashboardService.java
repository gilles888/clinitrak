package be.clinitrak.ethics.service;

import be.clinitrak.ethics.domain.entity.Meeting;
import be.clinitrak.ethics.domain.enums.MeetingStatus;
import be.clinitrak.ethics.domain.enums.ReviewDecision;
import be.clinitrak.ethics.domain.repository.AnnualReportRepository;
import be.clinitrak.ethics.domain.repository.EthicsReviewRepository;
import be.clinitrak.ethics.domain.repository.MeetingRepository;
import be.clinitrak.ethics.dto.EthicsDashboardResponse;
import be.clinitrak.ethics.dto.EthicsReviewResponse;
import be.clinitrak.ethics.exception.EthicsException;
import be.clinitrak.ethics.mapper.EthicsMapper;
import be.clinitrak.ethics.tenant.TenantContext;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Service d'agrégation pour le tableau de bord du Comité d'Éthique.
 *
 * <p>Agrège les données de plusieurs repositories pour fournir une vue d'ensemble
 * des indicateurs clés du CE : avis en attente, réunions à venir,
 * rapports annuels dus ou en retard, et répartition des décisions.
 */
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class EthicsDashboardService {

    private final EthicsReviewRepository reviewRepository;
    private final MeetingRepository meetingRepository;
    private final AnnualReportRepository annualReportRepository;
    private final EthicsMapper ethicsMapper;

    /**
     * Calcule et retourne les indicateurs du tableau de bord CE pour le tenant courant.
     *
     * <p>Indicateurs calculés :
     * <ul>
     *   <li>Nombre d'avis en attente de décision</li>
     *   <li>Nombre d'avis soumis le mois dernier</li>
     *   <li>Nombre de réunions dans les 30 prochains jours</li>
     *   <li>Prochaine réunion planifiée</li>
     *   <li>Nombre de rapports annuels dus dans les 60 prochains jours</li>
     *   <li>Nombre de rapports annuels en retard</li>
     *   <li>Répartition des avis par décision</li>
     *   <li>5 derniers avis en attente</li>
     *   <li>{@code pendingSubmissions} : alias de pendingReviews pour le widget frontend</li>
     *   <li>{@code urgentDeadlines} : séances CE planifiées dans les 7 prochains jours</li>
     *   <li>{@code averageProcessingDays} : délai moyen en jours entre soumission et décision (défaut 38.0)</li>
     * </ul>
     *
     * @return réponse agrégée du tableau de bord
     * @throws EthicsException si le tenant courant n'est pas résolu
     */
    public EthicsDashboardResponse getDashboard() {
        UUID tenantId = resolveTenantId();
        LocalDate today = LocalDate.now();

        // Avis en attente
        long pendingReviews = reviewRepository.countPendingByTenantId(tenantId);

        // Avis soumis le mois dernier
        LocalDate monthStart = today.minusMonths(1).withDayOfMonth(1);
        LocalDate monthEnd = today.minusMonths(1).plusMonths(1).withDayOfMonth(1).minusDays(1);
        long reviewsLastMonth = reviewRepository.countByTenantIdAndSubmissionDateBetween(
            tenantId, monthStart, monthEnd
        );

        // Réunions à venir (30 prochains jours)
        long upcomingMeetings = meetingRepository.countByTenantIdAndStatusAndMeetingDateAfter(
            tenantId, MeetingStatus.PLANNED, today.minusDays(1)
        );

        // Prochaine réunion
        List<Meeting> nextMeetings = meetingRepository
            .findByTenantIdAndStatusAndMeetingDateAfterOrderByMeetingDate(
                tenantId, MeetingStatus.PLANNED, today.minusDays(1)
            );
        Meeting nextMeeting = nextMeetings.isEmpty() ? null : nextMeetings.get(0);

        // Rapports annuels dus dans les 60 prochains jours
        long annualReportsDue = annualReportRepository.countDueSoonByTenantId(
            tenantId, today.plusDays(60)
        );

        // Rapports annuels en retard
        long annualReportsOverdue = annualReportRepository.countOverdueByTenantId(tenantId);

        // Répartition par décision
        Map<String, Long> reviewsByDecision = buildDecisionMap(tenantId);

        // 5 derniers avis en attente
        List<EthicsReviewResponse> recentPending = reviewRepository
            .findRecentPendingByTenantId(tenantId, PageRequest.of(0, 5))
            .stream()
            .map(ethicsMapper::toResponse)
            .collect(Collectors.toList());

        // Séances CE dans les 7 prochains jours (deadlines urgentes)
        long urgentDeadlines = meetingRepository.countByTenantIdAndStatusAndMeetingDateBetween(
            tenantId, MeetingStatus.PLANNED, today, today.plusDays(7)
        );

        // Délai moyen de traitement (soumission → décision) — défaut 38.0 si aucune donnée
        Double rawAverage = reviewRepository.computeAverageProcessingDays(tenantId);
        double averageProcessingDays = rawAverage != null ? rawAverage : 38.0;

        log.debug("Dashboard CE calculé pour tenant {} : {} avis en attente, {} réunions à venir, {} délais urgents",
            tenantId, pendingReviews, upcomingMeetings, urgentDeadlines);

        return new EthicsDashboardResponse(
            pendingReviews,
            reviewsLastMonth,
            upcomingMeetings,
            nextMeeting,
            annualReportsDue,
            annualReportsOverdue,
            reviewsByDecision,
            recentPending,
            pendingReviews,      // pendingSubmissions = alias de pendingReviews
            urgentDeadlines,
            averageProcessingDays
        );
    }

    // ----------------------------------------------------------------
    // Méthodes utilitaires privées
    // ----------------------------------------------------------------

    /**
     * Construit la map de répartition des avis par décision.
     *
     * @param tenantId identifiant du tenant
     * @return map ordonnée des décisions avec leur nombre
     */
    private Map<String, Long> buildDecisionMap(UUID tenantId) {
        List<Object[]> rows = reviewRepository.countByDecisionAndTenantId(tenantId);
        Map<String, Long> result = new LinkedHashMap<>();
        // Initialiser avec toutes les décisions à 0
        for (ReviewDecision decision : ReviewDecision.values()) {
            result.put(decision.name(), 0L);
        }
        // Remplir avec les vraies valeurs
        for (Object[] row : rows) {
            if (row[0] != null) {
                result.put(row[0].toString(), (Long) row[1]);
            }
        }
        return result;
    }

    /**
     * Résout l'UUID du tenant courant depuis le {@link TenantContext}.
     *
     * @return UUID du tenant
     * @throws EthicsException si le tenant n'est pas résolu
     */
    private UUID resolveTenantId() {
        String tenantStr = TenantContext.getTenantId();
        if (tenantStr == null || tenantStr.isBlank()) {
            throw new EthicsException("Tenant non résolu — vérifiez le header X-Tenant-ID ou le token JWT");
        }
        try {
            return UUID.fromString(tenantStr);
        } catch (IllegalArgumentException e) {
            throw new EthicsException("Identifiant de tenant invalide : " + tenantStr);
        }
    }
}
