package be.clinitrak.admin.domain.entity;

import be.clinitrak.admin.domain.enums.ModuleType;
import be.clinitrak.admin.domain.enums.SubscriptionType;
import be.clinitrak.admin.domain.enums.TenantStatus;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;

/**
 * Tenant de la plateforme CliniTrak.
 *
 * <p>Représente un établissement hospitalier ou une institution de recherche
 * utilisant la plateforme. Chaque tenant est isolé des autres (multi-tenancy).
 *
 * <p>La configuration libre du tenant est stockée en JSON dans la colonne
 * {@code configuration} de type JSONB.
 */
@Getter
@Setter
@Entity
@Table(name = "admin_tenants")
public class AdminTenant extends BaseEntity {

    /** Nom complet de l'établissement. */
    @Column(name = "name", nullable = false, length = 255)
    private String name;

    /** Slug unique utilisé dans les URLs et comme identifiant tenant (ex: saintluc). */
    @Column(name = "slug", nullable = false, unique = true, length = 100)
    private String slug;

    /** Domaine web de l'établissement (facultatif). */
    @Column(name = "domain", length = 255)
    private String domain;

    /** URL du logo de l'établissement. */
    @Column(name = "logo_url", length = 500)
    private String logoUrl;

    /** Modules fonctionnels activés pour ce tenant. */
    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "admin_tenant_active_modules",
        joinColumns = @JoinColumn(name = "tenant_id"))
    @Column(name = "module")
    @Enumerated(EnumType.STRING)
    private Set<ModuleType> activeModules = new HashSet<>();

    /**
     * Configuration JSON libre du tenant (stockée en JSONB PostgreSQL).
     *
     * <p>Contient les préférences avancées : format des numéros CE, timezone,
     * langue par défaut, quotas, fonctionnalités activées.
     */
    @Column(name = "configuration", columnDefinition = "jsonb")
    private String configuration;

    /** Type d'abonnement du tenant. */
    @Enumerated(EnumType.STRING)
    @Column(name = "subscription_type", nullable = false, length = 50)
    private SubscriptionType subscriptionType = SubscriptionType.BASIC;

    /** Statut courant du tenant. */
    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 50)
    private TenantStatus status = TenantStatus.ACTIVE;

    /** Date de création du tenant (colonne locale, distinct de BaseEntity.createdAt). */
    @Column(name = "created_at_local")
    private LocalDateTime createdAtLocal;
}
