package be.clinitrak.ctc.service;

import be.clinitrak.ctc.domain.enums.ContractStatus;
import be.clinitrak.ctc.domain.enums.EventStatus;
import be.clinitrak.ctc.domain.enums.RequestStatus;
import be.clinitrak.ctc.domain.enums.Severity;
import be.clinitrak.ctc.domain.enums.StatisticsStatus;
import be.clinitrak.ctc.domain.enums.VisitStatus;
import be.clinitrak.ctc.domain.repository.*;
import be.clinitrak.ctc.dto.CTCDashboardResponse;
import be.clinitrak.ctc.dto.TrialDeskRequestResponse;
import be.clinitrak.ctc.exception.CtcException;
import be.clinitrak.ctc.mapper.CtcMapper;
import be.clinitrak.ctc.tenant.TenantContext;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Service d'agrégation pour le tableau de bord du Centre de Thérapie Cellulaire.
 *
 * <p>Agrège les données de tous les repositories CTC pour fournir une vue
 * d'ensemble des indicateurs clés : demandes en attente, visites planifiées,
 * événements qualité ouverts, contrats actifs et statistiques pendantes.
 */
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class CTCDashboardService {

    private final TrialDeskRequestRepository deskRequestRepository;
    private final MonitoringVisitRepository monitoringVisitRepository;
    private final FinancialContractRepository financialContractRepository;
    private final StatisticsRequestRepository statisticsRequestRepository;
    private final SponsorCUSLStudyRepository sponsorStudyRepository;
    private final QualityEventRepository qualityEventRepository;
    private final CtcMapper mapper;

    /**
     * Calcule et retourne les indicateurs du tableau de bord CTC pour le tenant courant.
     *
     * <p>Indicateurs calculés :
     * <ul>
     *   <li>Nombre total de demandes desk actives</li>
     *   <li>Nombre de demandes en statut PENDING</li>
     *   <li>Nombre de visites de monitoring planifiées</li>
     *   <li>Nombre d'événements qualité ouverts</li>
     *   <li>Nombre d'événements qualité de sévérité CRITICAL</li>
     *   <li>Nombre de contrats financiers actifs</li>
     *   <li>Nombre de demandes statistiques en attente</li>
     *   <li>Nombre d'études gérées en tant que sponsor CUSL</li>
     *   <li>5 dernières demandes desk soumises</li>
     *   <li>Dossiers actifs : demandes ASSIGNED ou IN_PROGRESS (widget frontend)</li>
     *   <li>Demandes PENDING sans assigné : en attente de prise en charge (widget frontend)</li>
     * </ul>
     *
     * @return réponse agrégée du tableau de bord CTC
     * @throws CtcException si le tenant courant n'est pas résolu
     */
    public CTCDashboardResponse getDashboard() {
        String tenantId = resolveTenantId();

        List<TrialDeskRequestResponse> allRequests = deskRequestRepository
            .findByTenantIdAndDeletedFalse(tenantId)
            .stream()
            .map(mapper::toResponse)
            .collect(Collectors.toList());

        long totalDeskRequests = allRequests.size();

        long pendingRequests = allRequests.stream()
            .filter(r -> r.status() == RequestStatus.PENDING)
            .count();

        long plannedVisits = monitoringVisitRepository
            .findByTenantIdAndStatusAndDeletedFalse(tenantId, VisitStatus.PLANNED)
            .size();

        long openQualityEvents = qualityEventRepository
            .findByTenantIdAndStatusAndDeletedFalse(tenantId, EventStatus.OPEN)
            .size();

        long criticalEvents = qualityEventRepository
            .findByTenantIdAndDeletedFalse(tenantId)
            .stream()
            .filter(e -> e.getSeverity() == Severity.CRITICAL && e.getStatus() != EventStatus.CLOSED)
            .count();

        long activeContracts = financialContractRepository
            .findByTenantIdAndDeletedFalse(tenantId)
            .stream()
            .filter(c -> c.getStatus() == ContractStatus.ACTIVE)
            .count();

        long pendingStatistics = statisticsRequestRepository
            .findByTenantIdAndDeletedFalse(tenantId)
            .stream()
            .filter(s -> s.getStatus() == StatisticsStatus.PENDING)
            .count();

        long sponsorStudies = sponsorStudyRepository
            .findByTenantIdAndDeletedFalse(tenantId)
            .size();

        // 5 dernières demandes desk (triées par date de création décroissante)
        List<TrialDeskRequestResponse> recentRequests = allRequests.stream()
            .sorted((a, b) -> b.createdAt() != null && a.createdAt() != null
                ? b.createdAt().compareTo(a.createdAt()) : 0)
            .limit(5)
            .collect(Collectors.toList());

        // Dossiers actifs : demandes en cours de traitement (ASSIGNED ou IN_PROGRESS)
        long activeDossiers = allRequests.stream()
            .filter(r -> r.status() == RequestStatus.ASSIGNED || r.status() == RequestStatus.IN_PROGRESS)
            .count();

        // Demandes PENDING non encore assignées — widget frontend "en attente de prise en charge"
        long pendingManufacturing = allRequests.stream()
            .filter(r -> r.status() == RequestStatus.PENDING
                      && (r.assignedTo() == null || r.assignedTo().isBlank()))
            .count();

        log.debug("Dashboard CTC calculé pour tenant {} : {} demandes, {} visites planifiées, {} dossiers actifs",
            tenantId, totalDeskRequests, plannedVisits, activeDossiers);

        return new CTCDashboardResponse(
            totalDeskRequests,
            pendingRequests,
            plannedVisits,
            openQualityEvents,
            criticalEvents,
            activeContracts,
            pendingStatistics,
            sponsorStudies,
            recentRequests,
            activeDossiers,
            pendingManufacturing
        );
    }

    // ----------------------------------------------------------------
    // Méthodes utilitaires privées
    // ----------------------------------------------------------------

    /**
     * Résout l'identifiant du tenant courant depuis le {@link TenantContext}.
     *
     * @return identifiant du tenant sous forme de String
     * @throws CtcException si le tenant n'est pas résolu
     */
    private String resolveTenantId() {
        String tenantStr = TenantContext.getTenantId();
        if (tenantStr == null || tenantStr.isBlank()) {
            throw new CtcException("Tenant non résolu — vérifiez le header X-Tenant-ID ou le token JWT");
        }
        return tenantStr;
    }
}
