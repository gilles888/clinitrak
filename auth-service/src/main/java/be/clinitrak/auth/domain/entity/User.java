package be.clinitrak.auth.domain.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

/**
 * Utilisateur de la plateforme CliniTrak.
 *
 * <p>Un utilisateur appartient à un tenant et possède un ou plusieurs rôles.
 * Le champ {@code tenantId} est l'identifiant du tenant courant de l'utilisateur.
 */
@Entity
@Table(name = "users", indexes = {
    @Index(name = "idx_user_email", columnList = "email"),
    @Index(name = "idx_user_tenant", columnList = "tenant_id")
})
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class User extends BaseEntity {

    /** Email = identifiant de connexion unique par tenant. */
    @Column(name = "email", nullable = false, length = 255)
    private String email;

    /** Mot de passe hashé (BCrypt). */
    @Column(name = "password_hash", nullable = false)
    private String passwordHash;

    @Column(name = "first_name", nullable = false, length = 100)
    private String firstName;

    @Column(name = "last_name", nullable = false, length = 100)
    private String lastName;

    /** Tenant auquel appartient cet utilisateur. */
    @Column(name = "tenant_id", nullable = false)
    private UUID tenantId;

    /** Rôles attribués à l'utilisateur dans son tenant. */
    @ManyToMany(fetch = FetchType.EAGER)
    @JoinTable(
        name = "user_roles",
        joinColumns = @JoinColumn(name = "user_id"),
        inverseJoinColumns = @JoinColumn(name = "role_id")
    )
    @Builder.Default
    private Set<Role> roles = new HashSet<>();

    /** Compte activé (confirmation email). */
    @Builder.Default
    @Column(name = "enabled", nullable = false)
    private boolean enabled = true;

    /** Compte verrouillé après trop de tentatives. */
    @Builder.Default
    @Column(name = "account_locked", nullable = false)
    private boolean accountLocked = false;

    /** Timestamp du dernier login réussi. */
    @Column(name = "last_login_at")
    private Instant lastLoginAt;

    /** Nombre de tentatives de connexion échouées consécutives. */
    @Builder.Default
    @Column(name = "failed_login_attempts", nullable = false)
    private int failedLoginAttempts = 0;

    /** Timestamp de l'expiration du verrouillage (null = indéfini). */
    @Column(name = "locked_until")
    private Instant lockedUntil;
}
