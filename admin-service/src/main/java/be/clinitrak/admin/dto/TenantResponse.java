package be.clinitrak.admin.dto;

import be.clinitrak.admin.domain.enums.ModuleType;
import be.clinitrak.admin.domain.enums.SubscriptionType;
import be.clinitrak.admin.domain.enums.TenantStatus;

import java.time.LocalDateTime;
import java.util.Set;
import java.util.UUID;

/**
 * DTO de représentation complète d'un tenant.
 *
 * @param id                    UUID du tenant
 * @param name                  nom de l'établissement
 * @param slug                  slug unique
 * @param domain                domaine web
 * @param logoUrl               URL du logo
 * @param activeModules         modules activés
 * @param configuration         configuration JSON brute
 * @param subscriptionType      type d'abonnement
 * @param subscriptionTypeLabel libellé du type d'abonnement
 * @param status                statut courant
 * @param statusLabel           libellé du statut
 * @param createdAt             date de création
 */
public record TenantResponse(
    UUID id,
    String name,
    String slug,
    String domain,
    String logoUrl,
    Set<ModuleType> activeModules,
    String configuration,
    SubscriptionType subscriptionType,
    String subscriptionTypeLabel,
    TenantStatus status,
    String statusLabel,
    LocalDateTime createdAt
) {}
