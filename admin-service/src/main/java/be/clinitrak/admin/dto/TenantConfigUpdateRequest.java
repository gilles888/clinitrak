package be.clinitrak.admin.dto;

/**
 * DTO de mise à jour de la configuration d'un tenant.
 *
 * @param ceNumberFormat  format des numéros CE (ex: "YYYY/NNNN")
 * @param timezone        fuseau horaire (ex: "Europe/Brussels")
 * @param defaultLanguage langue par défaut (ex: "fr")
 * @param maxUsers        nombre maximum d'utilisateurs autorisés
 * @param storageQuotaGb  quota de stockage en gigaoctets
 */
public record TenantConfigUpdateRequest(
    String ceNumberFormat,
    String timezone,
    String defaultLanguage,
    Integer maxUsers,
    Integer storageQuotaGb
) {}
