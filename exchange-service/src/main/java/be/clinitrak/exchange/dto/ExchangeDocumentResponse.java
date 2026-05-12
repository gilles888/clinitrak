package be.clinitrak.exchange.dto;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * DTO de représentation d'un document attaché à une demande d'échange.
 *
 * @param id         UUID du document
 * @param fileName   nom original du fichier
 * @param fileSize   taille en octets
 * @param mimeType   type MIME du fichier
 * @param uploadedAt date de téléversement
 */
public record ExchangeDocumentResponse(
    UUID id,
    String fileName,
    Long fileSize,
    String mimeType,
    LocalDateTime uploadedAt
) {}
