package be.clinitrak.exchange.dto;

import jakarta.validation.constraints.NotBlank;

/**
 * DTO de création d'un message dans une demande d'échange.
 *
 * @param content        contenu textuel du message
 * @param attachmentPath chemin vers une pièce jointe optionnelle
 */
public record ExchangeMessageCreateRequest(
    @NotBlank String content,
    String attachmentPath
) {}
