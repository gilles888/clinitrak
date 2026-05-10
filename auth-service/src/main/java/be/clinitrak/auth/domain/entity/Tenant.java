package be.clinitrak.auth.domain.entity;

import jakarta.persistence.*;
import lombok.*;

/**
 * Représente un tenant (institution hospitalière) dans le système multi-tenant.
 *
 * <p>Chaque tenant est identifié par un slug unique utilisé comme discriminant
 * dans le header {@code X-Tenant-ID} ou via le sous-domaine.
 */
@Entity
@Table(name = "tenants", uniqueConstraints = {
    @UniqueConstraint(name = "uk_tenant_slug", columnNames = "slug")
})
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Tenant extends BaseEntity {

    /** Identifiant court unique du tenant (ex: "saintluc", "erasme"). */
    @Column(name = "slug", nullable = false, length = 63)
    private String slug;

    /** Nom complet de l'institution. */
    @Column(name = "name", nullable = false, length = 255)
    private String name;

    /** Domaine personnalisé du tenant (optionnel). */
    @Column(name = "domain", length = 255)
    private String domain;

    /** Schéma PostgreSQL dédié (optionnel, pour isolation forte). */
    @Column(name = "db_schema", length = 63)
    private String dbSchema;

    /** Email de contact principal. */
    @Column(name = "contact_email", length = 255)
    private String contactEmail;

    /** Statut d'activation du tenant. */
    @Builder.Default
    @Column(name = "active", nullable = false)
    private boolean active = true;

    /** Paramètres JSON spécifiques au tenant (timezone, locale, etc.). */
    @Column(name = "settings", columnDefinition = "jsonb")
    private String settings;
}
