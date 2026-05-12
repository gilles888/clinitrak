package be.clinitrak.admin.dto;

import java.util.Map;

/**
 * DTO de statistiques d'utilisation d'un tenant.
 *
 * @param tenantId      identifiant du tenant
 * @param tenantName    nom du tenant
 * @param userCount     nombre total d'utilisateurs
 * @param studyCount    nombre total d'études
 * @param activeStudies nombre d'études en cours
 * @param moduleUsage   utilisation par module (nom → nombre d'entités)
 */
public record TenantStatisticsResponse(
    String tenantId,
    String tenantName,
    long userCount,
    long studyCount,
    long activeStudies,
    Map<String, Long> moduleUsage
) {}
