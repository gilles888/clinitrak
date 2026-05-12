package be.clinitrak.admin.dto;

import java.util.Map;

/**
 * Classe Java immuable représentant la configuration JSON d'un tenant.
 *
 * <p>Utilisée pour désérialiser le champ JSONB {@code configuration} de {@code AdminTenant}
 * lors des opérations de lecture et de mise à jour de configuration.
 *
 * @param ceNumberFormat    format des numéros CE (ex: "YYYY/NNNN")
 * @param timezone          fuseau horaire IANA (ex: "Europe/Brussels")
 * @param defaultLanguage   langue par défaut (ex: "fr", "en")
 * @param maxUsers          nombre maximum d'utilisateurs autorisés
 * @param storageQuotaGb    quota de stockage en gigaoctets
 * @param enabledFeatures   map des fonctionnalités activées (nom → booléen)
 */
public record TenantConfiguration(
    String ceNumberFormat,
    String timezone,
    String defaultLanguage,
    int maxUsers,
    int storageQuotaGb,
    Map<String, Boolean> enabledFeatures
) {}
