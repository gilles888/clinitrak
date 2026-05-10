package be.clinitrak.ctc.service;

import be.clinitrak.ctc.domain.entity.QualityEvent;
import be.clinitrak.ctc.domain.enums.EventStatus;
import be.clinitrak.ctc.domain.repository.QualityEventRepository;
import be.clinitrak.ctc.dto.QualityEventCreateRequest;
import be.clinitrak.ctc.dto.QualityEventResponse;
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
 * Service métier pour la gestion des événements qualité CTC.
 *
 * <p>Toutes les méthodes opèrent dans le contexte du tenant courant récupéré
 * via {@link TenantContext#getTenantId()}. Si le tenant n'est pas résolu,
 * une {@link CtcException} est levée immédiatement.
 */
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class QualityEventService {

    private final QualityEventRepository repository;
    private final CtcMapper mapper;

    /**
     * Retourne tous les événements qualité actifs du tenant courant.
     *
     * @param tenantId identifiant du tenant courant
     * @return liste des réponses d'événements qualité
     */
    public List<QualityEventResponse> getAll(String tenantId) {
        return repository.findByTenantIdAndDeletedFalse(tenantId)
            .stream()
            .map(mapper::toResponse)
            .collect(Collectors.toList());
    }

    /**
     * Crée un nouvel événement qualité avec statut OPEN.
     *
     * @param request  DTO de création de l'événement
     * @param tenantId identifiant du tenant courant
     * @return réponse complète de l'événement créé
     */
    @Transactional
    public QualityEventResponse create(QualityEventCreateRequest request, String tenantId) {
        QualityEvent entity = new QualityEvent();
        entity.setStudyId(request.studyId());
        entity.setEventType(request.eventType());
        entity.setEventDate(request.eventDate());
        entity.setSeverity(request.severity());
        entity.setDescription(request.description());
        entity.setStatus(EventStatus.OPEN);
        entity.setTenantId(tenantId);

        QualityEvent saved = repository.save(entity);
        log.info("Événement qualité créé : {} de type {} (tenant: {})",
            saved.getId(), saved.getEventType(), tenantId);
        return mapper.toResponse(saved);
    }
}
