package be.clinitrak.admin.dto;

import be.clinitrak.admin.domain.enums.ModuleType;
import be.clinitrak.admin.domain.enums.SubscriptionType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

import java.util.Set;

/**
 * DTO de création d'un nouveau tenant.
 *
 * @param name             nom complet de l'établissement
 * @param slug             slug unique (format : lettres minuscules, chiffres, tirets)
 * @param domain           domaine web (facultatif)
 * @param activeModules    modules à activer (peut être null pour le set par défaut)
 * @param subscriptionType type d'abonnement
 */
public record TenantCreateRequest(
    @NotBlank String name,
    @NotBlank @Pattern(regexp = "[a-z0-9-]+", message = "Le slug ne peut contenir que des lettres minuscules, chiffres et tirets") String slug,
    String domain,
    Set<ModuleType> activeModules,
    SubscriptionType subscriptionType
) {}
