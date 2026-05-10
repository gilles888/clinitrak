package be.clinitrak.ctc.service;

import be.clinitrak.ctc.domain.entity.*;
import be.clinitrak.ctc.domain.repository.*;
import be.clinitrak.ctc.dto.StudyTimelineResponse;
import be.clinitrak.ctc.dto.TimelineEvent;
import be.clinitrak.ctc.exception.CtcException;
import be.clinitrak.ctc.tenant.TenantContext;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * Service de construction de la timeline d'une étude clinique.
 *
 * <p>Agrège tous les événements CTC liés à une étude donnée
 * (visites, contrats, événements qualité, demandes desk) et les trie
 * chronologiquement pour affichage dans l'interface utilisateur.
 */
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class StudyTimelineService {

    private final TrialDeskRequestRepository deskRequestRepository;
    private final MonitoringVisitRepository monitoringVisitRepository;
    private final FinancialContractRepository financialContractRepository;
    private final QualityEventRepository qualityEventRepository;

    /**
     * Construit la timeline complète d'une étude pour le tenant courant.
     *
     * <p>Collecte les événements des 4 sources suivantes :
     * <ul>
     *   <li>Demandes desk : date de soumission</li>
     *   <li>Visites de monitoring : date de visite</li>
     *   <li>Contrats financiers : date de contrat</li>
     *   <li>Événements qualité : date de l'événement</li>
     * </ul>
     * Les événements sont triés par date croissante.
     *
     * @param studyId  identifiant de l'étude (référence study-service)
     * @param tenantId identifiant du tenant courant
     * @return timeline complète de l'étude
     * @throws CtcException si le tenant courant n'est pas résolu
     */
    public StudyTimelineResponse getTimeline(String studyId, String tenantId) {
        List<TimelineEvent> events = new ArrayList<>();

        // Demandes desk
        deskRequestRepository.findByStudyIdAndTenantIdAndDeletedFalse(studyId, tenantId)
            .forEach(r -> events.add(new TimelineEvent(
                r.getStudyId(),
                "DESK_REQUEST",
                r.getRequestType().getLabel() + " — " + r.getDeskType().getLabel(),
                r.getNotes() != null ? r.getNotes() : "Demandeur : " + r.getRequestorName(),
                r.getRequestDate(),
                r.getPriority() != null ? r.getPriority().getLabel() : null
            )));

        // Visites de monitoring
        monitoringVisitRepository.findByStudyIdAndTenantIdAndDeletedFalse(studyId, tenantId)
            .forEach(v -> events.add(new TimelineEvent(
                v.getStudyId(),
                "MONITORING_VISIT",
                "Visite " + v.getVisitType().getLabel(),
                "CRA : " + v.getMonitorName() + (v.getFindings() != null ? " — " + v.getFindings() : ""),
                v.getVisitDate(),
                null
            )));

        // Contrats financiers
        financialContractRepository.findByStudyIdAndTenantIdAndDeletedFalse(studyId, tenantId)
            .forEach(c -> events.add(new TimelineEvent(
                c.getStudyId(),
                "FINANCIAL_CONTRACT",
                c.getContractType().getLabel() + " — " + c.getCurrency().getLabel(),
                "Montant : " + c.getAmount() + " " + c.getCurrency().name(),
                c.getContractDate(),
                null
            )));

        // Événements qualité
        qualityEventRepository.findByStudyIdAndTenantIdAndDeletedFalse(studyId, tenantId)
            .forEach(e -> events.add(new TimelineEvent(
                e.getStudyId(),
                "QUALITY_EVENT",
                e.getEventType().getLabel() + " — " + e.getSeverity().getLabel(),
                e.getDescription(),
                e.getEventDate(),
                e.getSeverity().getLabel()
            )));

        // Tri chronologique par date d'événement (null en dernier)
        events.sort(Comparator.comparing(
            TimelineEvent::eventDate,
            Comparator.nullsLast(Comparator.naturalOrder())
        ));

        log.debug("Timeline construite pour étude {} : {} événements (tenant: {})",
            studyId, events.size(), tenantId);

        return new StudyTimelineResponse(studyId, events);
    }
}
