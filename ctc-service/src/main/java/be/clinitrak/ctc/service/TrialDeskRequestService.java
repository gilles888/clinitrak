package be.clinitrak.ctc.service;

import be.clinitrak.ctc.domain.entity.TrialDeskRequest;
import be.clinitrak.ctc.domain.enums.RequestStatus;
import be.clinitrak.ctc.domain.repository.TrialDeskRequestRepository;
import be.clinitrak.ctc.dto.AssignRequest;
import be.clinitrak.ctc.dto.StatusUpdateRequest;
import be.clinitrak.ctc.dto.TrialDeskRequestCreateRequest;
import be.clinitrak.ctc.dto.TrialDeskRequestResponse;
import be.clinitrak.ctc.exception.CtcException;
import be.clinitrak.ctc.exception.CtcNotFoundException;
import be.clinitrak.ctc.mapper.CtcMapper;
import be.clinitrak.ctc.tenant.TenantContext;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Service métier pour la gestion des demandes desk CTC.
 *
 * <p>Toutes les méthodes opèrent dans le contexte du tenant courant récupéré
 * via {@link TenantContext#getTenantId()}. Si le tenant n'est pas résolu,
 * une {@link CtcException} est levée immédiatement.
 */
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class TrialDeskRequestService {

    private final TrialDeskRequestRepository repository;
    private final CtcMapper mapper;

    /**
     * Retourne toutes les demandes desk actives du tenant courant.
     *
     * @param tenantId identifiant du tenant courant
     * @return liste des réponses de demandes desk
     */
    public List<TrialDeskRequestResponse> getAll(String tenantId) {
        return repository.findByTenantIdAndDeletedFalse(tenantId)
            .stream()
            .map(mapper::toResponse)
            .collect(Collectors.toList());
    }

    /**
     * Crée une nouvelle demande desk avec statut PENDING et date du jour.
     *
     * @param request  DTO de création de la demande
     * @param tenantId identifiant du tenant courant
     * @return réponse complète de la demande créée
     */
    @Transactional
    public TrialDeskRequestResponse create(TrialDeskRequestCreateRequest request, String tenantId) {
        TrialDeskRequest entity = new TrialDeskRequest();
        entity.setStudyId(request.studyId());
        entity.setDeskType(request.deskType());
        entity.setRequestDate(LocalDate.now());
        entity.setRequestorName(request.requestorName());
        entity.setRequestorEmail(request.requestorEmail());
        entity.setRequestorOrganization(request.requestorOrganization());
        entity.setRequestType(request.requestType());
        entity.setStatus(RequestStatus.PENDING);
        entity.setPriority(request.priority() != null ? request.priority() : entity.getPriority());
        entity.setDeadline(request.deadline());
        entity.setNotes(request.notes());
        entity.setTenantId(tenantId);

        TrialDeskRequest saved = repository.save(entity);
        log.info("Demande desk créée : {} pour étude {} (tenant: {})", saved.getId(), saved.getStudyId(), tenantId);
        return mapper.toResponse(saved);
    }

    /**
     * Assigne une demande desk à un responsable et met à jour la priorité et l'échéance.
     *
     * <p>Le statut passe automatiquement à ASSIGNED lors de l'assignation.
     *
     * @param id           identifiant UUID de la demande
     * @param assignRequest données d'assignation (assigné, priorité, deadline)
     * @param tenantId     identifiant du tenant courant
     * @return réponse complète de la demande mise à jour
     * @throws CtcNotFoundException si la demande n'existe pas dans ce tenant
     */
    @Transactional
    public TrialDeskRequestResponse assign(UUID id, AssignRequest assignRequest, String tenantId) {
        TrialDeskRequest entity = findOrThrow(id, tenantId);
        entity.setAssignedTo(assignRequest.assignedTo());
        if (assignRequest.priority() != null) {
            entity.setPriority(assignRequest.priority());
        }
        if (assignRequest.deadline() != null) {
            entity.setDeadline(assignRequest.deadline());
        }
        entity.setStatus(RequestStatus.ASSIGNED);

        TrialDeskRequest saved = repository.save(entity);
        log.info("Demande desk {} assignée à {} (tenant: {})", id, assignRequest.assignedTo(), tenantId);
        return mapper.toResponse(saved);
    }

    /**
     * Met à jour le statut d'une demande desk.
     *
     * @param id            identifiant UUID de la demande
     * @param statusRequest nouveau statut et notes optionnelles
     * @param tenantId      identifiant du tenant courant
     * @return réponse complète de la demande mise à jour
     * @throws CtcNotFoundException si la demande n'existe pas dans ce tenant
     */
    @Transactional
    public TrialDeskRequestResponse updateStatus(UUID id, StatusUpdateRequest statusRequest, String tenantId) {
        TrialDeskRequest entity = findOrThrow(id, tenantId);
        entity.setStatus(statusRequest.status());
        if (statusRequest.notes() != null) {
            entity.setNotes(statusRequest.notes());
        }

        TrialDeskRequest saved = repository.save(entity);
        log.info("Statut demande desk {} mis à jour : {} (tenant: {})", id, statusRequest.status(), tenantId);
        return mapper.toResponse(saved);
    }

    // ----------------------------------------------------------------
    // Méthodes utilitaires privées
    // ----------------------------------------------------------------

    /**
     * Résout l'UUID du tenant courant depuis le {@link TenantContext}.
     *
     * @return identifiant du tenant sous forme de String
     * @throws CtcException si le tenant n'est pas résolu
     */
    public static String resolveTenantId() {
        String tenantStr = TenantContext.getTenantId();
        if (tenantStr == null || tenantStr.isBlank()) {
            throw new CtcException("Tenant non résolu — vérifiez le header X-Tenant-ID ou le token JWT");
        }
        return tenantStr;
    }

    /**
     * Recherche une demande par ID et tenant, ou lève une exception si introuvable.
     *
     * @param id       identifiant de la demande
     * @param tenantId identifiant du tenant
     * @return entité TrialDeskRequest trouvée
     * @throws CtcNotFoundException si la demande n'existe pas ou est supprimée
     */
    private TrialDeskRequest findOrThrow(UUID id, String tenantId) {
        return repository.findByTenantIdAndDeletedFalse(tenantId)
            .stream()
            .filter(r -> r.getId().equals(id))
            .findFirst()
            .orElseThrow(() -> new CtcNotFoundException("Demande desk", id));
    }
}
