package be.clinitrak.exchange.service;

import be.clinitrak.exchange.domain.entity.ExchangeMessage;
import be.clinitrak.exchange.domain.entity.ExchangeRequest;
import be.clinitrak.exchange.domain.enums.SenderType;
import be.clinitrak.exchange.domain.repository.ExchangeMessageRepository;
import be.clinitrak.exchange.domain.repository.ExchangeRequestRepository;
import be.clinitrak.exchange.dto.ExchangeMessageResponse;
import be.clinitrak.exchange.exception.ExchangeNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;
import java.util.UUID;

/**
 * Service métier pour la gestion des messages dans les demandes d'échange.
 *
 * <p>Supporte la communication bidirectionnelle entre les utilisateurs internes
 * (staff hospitalier) et les utilisateurs externes (firmes, investigateurs).
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ExchangeMessageService {

    private final ExchangeMessageRepository exchangeMessageRepository;
    private final ExchangeRequestRepository exchangeRequestRepository;

    /**
     * Retourne tous les messages d'une demande d'échange pour un tenant, triés chronologiquement.
     *
     * @param requestId UUID de la demande d'échange
     * @param tenantId  identifiant du tenant
     * @return liste de messages triés par date d'envoi croissante
     */
    @Transactional(readOnly = true)
    public List<ExchangeMessageResponse> getByRequest(UUID requestId, String tenantId) {
        return exchangeMessageRepository
            .findByRequest_IdAndTenantIdAndDeletedFalseOrderBySentAtAsc(requestId, tenantId)
            .stream()
            .map(this::toResponse)
            .toList();
    }

    /**
     * Envoie un nouveau message dans une demande d'échange.
     *
     * @param requestId  UUID de la demande d'échange
     * @param senderId   UUID de l'expéditeur (utilisateur interne ou ExternalUser)
     * @param senderType type d'expéditeur (INTERNAL ou EXTERNAL)
     * @param content    contenu textuel du message
     * @param tenantId   identifiant du tenant
     * @return message créé
     * @throws ExchangeNotFoundException si la demande n'existe pas
     */
    @Transactional
    public ExchangeMessageResponse sendMessage(UUID requestId, String senderId, SenderType senderType,
                                               String content, String tenantId) {
        ExchangeRequest request = exchangeRequestRepository
            .findByIdAndTenantIdAndDeletedFalse(requestId, tenantId)
            .orElseThrow(() -> new ExchangeNotFoundException("Demande d'échange", requestId));

        ExchangeMessage message = new ExchangeMessage();
        message.setRequest(request);
        message.setSenderId(senderId);
        message.setSenderType(senderType);
        message.setContent(content);
        message.setSentAt(LocalDateTime.now());
        message.setTenantId(tenantId);

        ExchangeMessage saved = exchangeMessageRepository.save(message);
        log.debug("Message envoyé dans la demande {} par {} ({})", requestId, senderId, senderType);

        return toResponse(saved);
    }

    /**
     * Convertit une entité {@link ExchangeMessage} en DTO de réponse.
     *
     * @param message entité message
     * @return DTO de réponse
     */
    private ExchangeMessageResponse toResponse(ExchangeMessage message) {
        LocalDateTime createdAt = message.getCreatedAt() != null
            ? LocalDateTime.ofInstant(message.getCreatedAt(), ZoneId.systemDefault())
            : null;

        return new ExchangeMessageResponse(
            message.getId(),
            message.getRequest() != null ? message.getRequest().getId() : null,
            message.getSenderId(),
            message.getSenderType(),
            message.getSenderType() != null ? message.getSenderType().getLabel() : null,
            message.getContent(),
            message.getAttachmentPath(),
            message.getSentAt(),
            message.getReadAt(),
            createdAt
        );
    }
}
