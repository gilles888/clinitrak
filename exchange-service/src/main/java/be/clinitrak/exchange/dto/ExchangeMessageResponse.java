package be.clinitrak.exchange.dto;

import be.clinitrak.exchange.domain.enums.SenderType;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * DTO de représentation d'un message d'une demande d'échange.
 *
 * @param id             UUID du message
 * @param requestId      UUID de la demande parente
 * @param senderId       UUID de l'expéditeur
 * @param senderType     type d'expéditeur (interne ou externe)
 * @param senderTypeLabel libellé du type d'expéditeur
 * @param content        contenu textuel
 * @param attachmentPath chemin vers la pièce jointe
 * @param sentAt         date d'envoi
 * @param readAt         date de lecture
 * @param createdAt      date de création en base
 */
public record ExchangeMessageResponse(
    UUID id,
    UUID requestId,
    String senderId,
    SenderType senderType,
    String senderTypeLabel,
    String content,
    String attachmentPath,
    LocalDateTime sentAt,
    LocalDateTime readAt,
    LocalDateTime createdAt
) {}
