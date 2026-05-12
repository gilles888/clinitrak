package be.clinitrak.admin.dto;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * DTO de représentation d'un log d'audit système.
 *
 * @param id         UUID du log
 * @param tenantId   identifiant du tenant
 * @param userId     identifiant de l'utilisateur
 * @param action     action effectuée
 * @param entityType type de l'entité
 * @param entityId   identifiant de l'entité
 * @param oldValue   valeur avant modification
 * @param newValue   valeur après modification
 * @param ipAddress  adresse IP du client
 * @param timestamp  timestamp de l'action
 */
public record AuditLogResponse(
    UUID id,
    String tenantId,
    String userId,
    String action,
    String entityType,
    String entityId,
    String oldValue,
    String newValue,
    String ipAddress,
    LocalDateTime timestamp
) {}
