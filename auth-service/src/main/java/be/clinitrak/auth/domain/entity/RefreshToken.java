package be.clinitrak.auth.domain.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;
import java.util.UUID;

/**
 * Token de rafraîchissement JWT persisté en base.
 *
 * <p>Durée de vie : 7 jours. Un seul refresh token actif par utilisateur par appareil.
 * Le token est invalidé lors du logout ou lors d'une rotation (refresh → nouveau token).
 */
@Entity
@Table(name = "refresh_tokens", indexes = {
    @Index(name = "idx_refresh_token_value", columnList = "token"),
    @Index(name = "idx_refresh_token_user", columnList = "user_id")
})
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RefreshToken extends BaseEntity {

    /** Valeur opaque du token (UUID v4). */
    @Column(name = "token", nullable = false, unique = true, length = 512)
    private String token;

    /** Utilisateur propriétaire du token. */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    /** Tenant dans lequel le token a été émis. */
    @Column(name = "tenant_id", nullable = false)
    private UUID tenantId;

    /** Timestamp d'expiration. */
    @Column(name = "expires_at", nullable = false)
    private Instant expiresAt;

    /** Indique si le token a été révoqué. */
    @Builder.Default
    @Column(name = "revoked", nullable = false)
    private boolean revoked = false;

    /** User-Agent de l'appareil émetteur (pour audit). */
    @Column(name = "user_agent", length = 512)
    private String userAgent;

    /** IP source de l'émission du token. */
    @Column(name = "ip_address", length = 45)
    private String ipAddress;

    /** Indique si ce token est encore valide. */
    public boolean isExpired() {
        return Instant.now().isAfter(expiresAt);
    }

    /** Indique si ce token peut être utilisé pour un rafraîchissement. */
    public boolean isUsable() {
        return !revoked && !isExpired();
    }
}
