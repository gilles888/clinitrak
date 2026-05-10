package be.clinitrak.ctc.service;

import be.clinitrak.ctc.domain.entity.StatisticsRequest;
import be.clinitrak.ctc.domain.enums.StatisticsStatus;
import be.clinitrak.ctc.domain.repository.StatisticsRequestRepository;
import be.clinitrak.ctc.dto.StatisticsRequestCreateRequest;
import be.clinitrak.ctc.dto.StatisticsRequestResponse;
import be.clinitrak.ctc.exception.CtcException;
import be.clinitrak.ctc.mapper.CtcMapper;
import be.clinitrak.ctc.tenant.TenantContext;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Service métier pour la gestion des demandes d'analyses statistiques CTC.
 *
 * <p>Toutes les méthodes opèrent dans le contexte du tenant courant récupéré
 * via {@link TenantContext#getTenantId()}. Si le tenant n'est pas résolu,
 * une {@link CtcException} est levée immédiatement.
 */
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class StatisticsRequestService {

    private final StatisticsRequestRepository repository;
    private final CtcMapper mapper;

    /**
     * Retourne toutes les demandes statistiques actives du tenant courant.
     *
     * @param tenantId identifiant du tenant courant
     * @return liste des réponses de demandes statistiques
     */
    public List<StatisticsRequestResponse> getAll(String tenantId) {
        return repository.findByTenantIdAndDeletedFalse(tenantId)
            .stream()
            .map(mapper::toResponse)
            .collect(Collectors.toList());
    }

    /**
     * Crée une nouvelle demande statistique avec statut PENDING et date du jour.
     *
     * @param request  DTO de création de la demande
     * @param tenantId identifiant du tenant courant
     * @return réponse complète de la demande créée
     */
    @Transactional
    public StatisticsRequestResponse create(StatisticsRequestCreateRequest request, String tenantId) {
        StatisticsRequest entity = new StatisticsRequest();
        entity.setStudyId(request.studyId());
        entity.setRequestDate(LocalDate.now());
        entity.setRequestorName(request.requestorName());
        entity.setDeadline(request.deadline());
        entity.setAnalysisType(request.analysisType());
        entity.setDataFormat(request.dataFormat());
        entity.setStatus(StatisticsStatus.PENDING);
        entity.setTenantId(tenantId);

        StatisticsRequest saved = repository.save(entity);
        log.info("Demande statistique créée : {} pour étude {} (tenant: {})",
            saved.getId(), saved.getStudyId(), tenantId);
        return mapper.toResponse(saved);
    }
}
