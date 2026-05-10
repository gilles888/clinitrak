package be.clinitrak.ctc.service;

import be.clinitrak.ctc.domain.entity.MonitoringVisit;
import be.clinitrak.ctc.domain.enums.VisitStatus;
import be.clinitrak.ctc.domain.repository.MonitoringVisitRepository;
import be.clinitrak.ctc.dto.MonitoringVisitCreateRequest;
import be.clinitrak.ctc.dto.MonitoringVisitResponse;
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
 * Service métier pour la gestion des visites de monitoring CTC.
 *
 * <p>Toutes les méthodes opèrent dans le contexte du tenant courant récupéré
 * via {@link TenantContext#getTenantId()}. Si le tenant n'est pas résolu,
 * une {@link CtcException} est levée immédiatement.
 */
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class MonitoringVisitService {

    private final MonitoringVisitRepository repository;
    private final CtcMapper mapper;

    /**
     * Retourne toutes les visites de monitoring actives du tenant courant.
     *
     * @param tenantId identifiant du tenant courant
     * @return liste des réponses de visites de monitoring
     */
    public List<MonitoringVisitResponse> getAll(String tenantId) {
        return repository.findByTenantIdAndDeletedFalse(tenantId)
            .stream()
            .map(mapper::toResponse)
            .collect(Collectors.toList());
    }

    /**
     * Crée une nouvelle visite de monitoring avec statut PLANNED.
     *
     * @param request  DTO de création de la visite
     * @param tenantId identifiant du tenant courant
     * @return réponse complète de la visite créée
     */
    @Transactional
    public MonitoringVisitResponse create(MonitoringVisitCreateRequest request, String tenantId) {
        MonitoringVisit entity = new MonitoringVisit();
        entity.setStudyId(request.studyId());
        entity.setVisitDate(request.visitDate());
        entity.setVisitType(request.visitType());
        entity.setMonitorName(request.monitorName());
        entity.setCorrectionDeadline(request.correctionDeadline());
        entity.setStatus(VisitStatus.PLANNED);
        entity.setTenantId(tenantId);

        MonitoringVisit saved = repository.save(entity);
        log.info("Visite de monitoring créée : {} pour étude {} (tenant: {})",
            saved.getId(), saved.getStudyId(), tenantId);
        return mapper.toResponse(saved);
    }
}
