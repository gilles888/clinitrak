package be.clinitrak.admin.dto;

import java.time.LocalDateTime;
import java.util.Map;

/**
 * DTO de santé globale de la plateforme.
 *
 * @param status    statut global ("UP" ou "DOWN")
 * @param services  map des statuts par service ("auth-service" → "UP", etc.)
 * @param checkedAt date et heure du contrôle
 */
public record SystemHealthResponse(
    String status,
    Map<String, String> services,
    LocalDateTime checkedAt
) {}
