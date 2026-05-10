package be.clinitrak.auth.domain.entity;

import jakarta.persistence.*;
import lombok.*;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

/**
 * Rôle fonctionnel regroupant un ensemble de permissions.
 *
 * <p>Les rôles système sont préfixés {@code ROLE_} conformément à Spring Security.
 * Un rôle peut être global (tenantId null) ou spécifique à un tenant.
 */
@Entity
@Table(name = "roles", uniqueConstraints = {
    @UniqueConstraint(name = "uk_role_name_tenant", columnNames = {"name", "tenant_id"})
})
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Role extends BaseEntity {

    /**
     * Rôles prédéfinis de la plateforme.
     *
     * <ul>
     *   <li>{@code ROLE_SUPER_ADMIN} — administrateur global de la plateforme</li>
     *   <li>{@code ROLE_ADMIN_TENANT} — administrateur d'un tenant</li>
     *   <li>{@code ROLE_CE_SECRETARY} — secrétariat du Comité d'Éthique</li>
     *   <li>{@code ROLE_CE_COORDINATOR} — coordinateur CE</li>
     *   <li>{@code ROLE_CTC_DESK} — secrétariat CTC</li>
     *   <li>{@code ROLE_CTC_CRA} — Clinical Research Associate</li>
     *   <li>{@code ROLE_CTC_PM} — Project Manager CTC</li>
     *   <li>{@code ROLE_CTC_COFI} — Coordinateur financier CTC</li>
     *   <li>{@code ROLE_PHARMACIST} — pharmacien responsable</li>
     *   <li>{@code ROLE_INVESTIGATOR} — investigateur principal/co-investigateur</li>
     *   <li>{@code ROLE_EXTERNAL} — collaborateur externe (lecture seule)</li>
     * </ul>
     */
    public enum SystemRole {
        ROLE_SUPER_ADMIN,
        ROLE_ADMIN_TENANT,
        ROLE_CE_SECRETARY,
        ROLE_CE_COORDINATOR,
        ROLE_CTC_DESK,
        ROLE_CTC_CRA,
        ROLE_CTC_PM,
        ROLE_CTC_COFI,
        ROLE_PHARMACIST,
        ROLE_INVESTIGATOR,
        ROLE_EXTERNAL
    }

    /** Nom du rôle (ex: "ROLE_INVESTIGATOR"). */
    @Column(name = "name", nullable = false, length = 100)
    private String name;

    /** Description du rôle. */
    @Column(name = "description", length = 500)
    private String description;

    /** Tenant propriétaire du rôle (null = rôle global). */
    @Column(name = "tenant_id")
    private UUID tenantId;

    /** Permissions associées à ce rôle. */
    @ManyToMany(fetch = FetchType.EAGER)
    @JoinTable(
        name = "role_permissions",
        joinColumns = @JoinColumn(name = "role_id"),
        inverseJoinColumns = @JoinColumn(name = "permission_id")
    )
    @Builder.Default
    private Set<Permission> permissions = new HashSet<>();
}
