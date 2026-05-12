package be.clinitrak.exchange.service;

import be.clinitrak.exchange.domain.entity.ExchangeDocument;
import be.clinitrak.exchange.domain.entity.ExchangeRequest;
import be.clinitrak.exchange.domain.entity.ExternalUser;
import be.clinitrak.exchange.domain.enums.ExchangeStatus;
import be.clinitrak.exchange.domain.repository.ExchangeDocumentRepository;
import be.clinitrak.exchange.domain.repository.ExchangeRequestRepository;
import be.clinitrak.exchange.domain.repository.ExternalUserRepository;
import be.clinitrak.exchange.dto.ExchangeDocumentResponse;
import be.clinitrak.exchange.dto.ExchangeRequestCreateRequest;
import be.clinitrak.exchange.dto.ExchangeRequestResponse;
import be.clinitrak.exchange.exception.ExchangeException;
import be.clinitrak.exchange.exception.ExchangeNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

/**
 * Service métier pour la gestion des demandes d'échange.
 *
 * <p>Gère le cycle de vie complet des demandes : création, soumission,
 * consultation, changement de statut et liaison avec une étude interne.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ExchangeRequestService {

    private final ExchangeRequestRepository exchangeRequestRepository;
    private final ExternalUserRepository externalUserRepository;
    private final ExchangeDocumentRepository exchangeDocumentRepository;

    /**
     * Crée une nouvelle demande d'échange avec le statut DRAFT.
     *
     * @param req            données de la demande
     * @param externalUserId UUID de l'utilisateur externe
     * @param tenantId       identifiant du tenant
     * @return demande créée
     * @throws ExchangeNotFoundException si l'utilisateur externe n'existe pas
     */
    @Transactional
    public ExchangeRequestResponse create(ExchangeRequestCreateRequest req, UUID externalUserId, String tenantId) {
        ExternalUser user = externalUserRepository.findById(externalUserId)
            .orElseThrow(() -> new ExchangeNotFoundException("Utilisateur externe", externalUserId));

        ExchangeRequest request = new ExchangeRequest();
        request.setExternalUser(user);
        request.setTargetModule(req.targetModule());
        request.setRequestType(req.requestType());
        request.setTitle(req.title());
        request.setDescription(req.description());
        request.setStatus(ExchangeStatus.DRAFT);
        request.setTenantId(tenantId);

        ExchangeRequest saved = exchangeRequestRepository.save(request);
        log.debug("Demande d'échange créée : {} pour l'utilisateur {}", saved.getId(), externalUserId);

        return toResponse(saved, List.of());
    }

    /**
     * Soumet formellement une demande d'échange (DRAFT → SUBMITTED).
     *
     * @param id             UUID de la demande
     * @param externalUserId UUID de l'utilisateur externe (vérification de propriété)
     * @param tenantId       identifiant du tenant
     * @return demande mise à jour
     * @throws ExchangeNotFoundException si la demande n'existe pas
     * @throws ExchangeException         si la demande n'appartient pas à l'utilisateur ou n'est pas en DRAFT
     */
    @Transactional
    public ExchangeRequestResponse submit(UUID id, UUID externalUserId, String tenantId) {
        ExchangeRequest request = findAndCheckOwnership(id, externalUserId, tenantId);

        if (request.getStatus() != ExchangeStatus.DRAFT) {
            throw new ExchangeException("Seules les demandes en DRAFT peuvent être soumises");
        }

        request.setStatus(ExchangeStatus.SUBMITTED);
        request.setSubmissionDate(LocalDateTime.now());

        ExchangeRequest saved = exchangeRequestRepository.save(request);
        log.info("Demande {} soumise par l'utilisateur {}", id, externalUserId);

        List<ExchangeDocumentResponse> docs = getDocumentResponses(saved.getId());
        return toResponse(saved, docs);
    }

    /**
     * Retourne l'historique des demandes d'un utilisateur externe pour un tenant.
     *
     * @param externalUserId UUID de l'utilisateur externe
     * @param tenantId       identifiant du tenant
     * @return liste des demandes de l'utilisateur
     */
    @Transactional(readOnly = true)
    public List<ExchangeRequestResponse> getByUser(UUID externalUserId, String tenantId) {
        return exchangeRequestRepository
            .findByExternalUser_IdAndTenantIdAndDeletedFalse(externalUserId, tenantId)
            .stream()
            .map(r -> toResponse(r, getDocumentResponses(r.getId())))
            .toList();
    }

    /**
     * Retourne le détail d'une demande avec ses documents et messages.
     *
     * @param id             UUID de la demande
     * @param externalUserId UUID de l'utilisateur externe (vérification de propriété)
     * @param tenantId       identifiant du tenant
     * @return demande détaillée
     * @throws ExchangeNotFoundException si la demande n'existe pas
     * @throws ExchangeException         si la demande n'appartient pas à l'utilisateur
     */
    @Transactional(readOnly = true)
    public ExchangeRequestResponse getById(UUID id, UUID externalUserId, String tenantId) {
        ExchangeRequest request = findAndCheckOwnership(id, externalUserId, tenantId);
        List<ExchangeDocumentResponse> docs = getDocumentResponses(request.getId());
        return toResponse(request, docs);
    }

    /**
     * Retourne toutes les demandes d'un tenant (vue interne).
     *
     * @param tenantId identifiant du tenant
     * @return liste de toutes les demandes du tenant
     */
    @Transactional(readOnly = true)
    public List<ExchangeRequestResponse> getAllForTenant(String tenantId) {
        return exchangeRequestRepository
            .findByTenantIdAndDeletedFalse(tenantId)
            .stream()
            .map(r -> toResponse(r, getDocumentResponses(r.getId())))
            .toList();
    }

    /**
     * Met à jour le statut d'une demande (réservé aux utilisateurs internes).
     *
     * @param id       UUID de la demande
     * @param status   nouveau statut
     * @param tenantId identifiant du tenant
     * @return demande mise à jour
     * @throws ExchangeNotFoundException si la demande n'existe pas
     */
    @Transactional
    public ExchangeRequestResponse updateStatus(UUID id, ExchangeStatus status, String tenantId) {
        ExchangeRequest request = exchangeRequestRepository
            .findByIdAndTenantIdAndDeletedFalse(id, tenantId)
            .orElseThrow(() -> new ExchangeNotFoundException("Demande d'échange", id));

        request.setStatus(status);
        ExchangeRequest saved = exchangeRequestRepository.save(request);
        log.info("Statut de la demande {} mis à jour : {}", id, status);

        List<ExchangeDocumentResponse> docs = getDocumentResponses(saved.getId());
        return toResponse(saved, docs);
    }

    /**
     * Lie une demande d'échange à une étude interne (après acceptation).
     *
     * @param id             UUID de la demande
     * @param internalStudyId identifiant de l'étude dans le study-service
     * @param tenantId       identifiant du tenant
     * @return demande mise à jour avec l'identifiant de l'étude
     * @throws ExchangeNotFoundException si la demande n'existe pas
     */
    @Transactional
    public ExchangeRequestResponse linkStudy(UUID id, String internalStudyId, String tenantId) {
        ExchangeRequest request = exchangeRequestRepository
            .findByIdAndTenantIdAndDeletedFalse(id, tenantId)
            .orElseThrow(() -> new ExchangeNotFoundException("Demande d'échange", id));

        request.setInternalStudyId(internalStudyId);
        ExchangeRequest saved = exchangeRequestRepository.save(request);
        log.info("Étude {} liée à la demande {}", internalStudyId, id);

        List<ExchangeDocumentResponse> docs = getDocumentResponses(saved.getId());
        return toResponse(saved, docs);
    }

    /**
     * Vérifie qu'une demande appartient bien à l'utilisateur externe donné.
     *
     * @param id             UUID de la demande
     * @param externalUserId UUID de l'utilisateur externe
     * @param tenantId       identifiant du tenant
     * @return demande trouvée
     * @throws ExchangeNotFoundException si la demande n'existe pas
     * @throws ExchangeException         si la demande n'appartient pas à l'utilisateur
     */
    private ExchangeRequest findAndCheckOwnership(UUID id, UUID externalUserId, String tenantId) {
        ExchangeRequest request = exchangeRequestRepository
            .findByIdAndTenantIdAndDeletedFalse(id, tenantId)
            .orElseThrow(() -> new ExchangeNotFoundException("Demande d'échange", id));

        if (!request.getExternalUser().getId().equals(externalUserId)) {
            throw new ExchangeException("Cette demande ne vous appartient pas");
        }

        return request;
    }

    /**
     * Retourne la liste des documents d'une demande sous forme de DTOs.
     *
     * @param requestId UUID de la demande
     * @return liste de DTOs de documents
     */
    private List<ExchangeDocumentResponse> getDocumentResponses(UUID requestId) {
        return exchangeDocumentRepository.findByRequest_IdAndDeletedFalse(requestId)
            .stream()
            .map(this::toDocumentResponse)
            .toList();
    }

    /**
     * Convertit une entité {@link ExchangeRequest} en DTO de réponse.
     *
     * @param request   entité demande
     * @param documents liste de DTOs de documents attachés
     * @return DTO de réponse
     */
    private ExchangeRequestResponse toResponse(ExchangeRequest request, List<ExchangeDocumentResponse> documents) {
        ExternalUser user = request.getExternalUser();
        String userName = user != null
            ? user.getFirstName() + " " + user.getLastName()
            : null;
        UUID userId = user != null ? user.getId() : null;

        return new ExchangeRequestResponse(
            request.getId(),
            userId,
            userName,
            request.getTargetModule(),
            request.getTargetModule() != null ? request.getTargetModule().getLabel() : null,
            request.getRequestType(),
            request.getRequestType() != null ? request.getRequestType().getLabel() : null,
            request.getTitle(),
            request.getDescription(),
            request.getSubmissionDate(),
            request.getStatus(),
            request.getStatus() != null ? request.getStatus().getLabel() : null,
            request.getInternalStudyId(),
            documents,
            request.getCreatedAt() != null
                ? LocalDateTime.ofInstant(request.getCreatedAt(), java.time.ZoneId.systemDefault())
                : null
        );
    }

    /**
     * Convertit une entité {@link ExchangeDocument} en DTO de réponse.
     *
     * @param doc entité document
     * @return DTO de réponse
     */
    private ExchangeDocumentResponse toDocumentResponse(ExchangeDocument doc) {
        return new ExchangeDocumentResponse(
            doc.getId(),
            doc.getFileName(),
            doc.getFileSize(),
            doc.getMimeType(),
            doc.getUploadedAt()
        );
    }
}
