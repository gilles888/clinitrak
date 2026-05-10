package be.clinitrak.auth.domain.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;
import java.util.UUID;

/**
 * Entrée d'audit traçant chaque action significative dans le système.
 *
 * <p>Alimenté automatiquement par {@link be.clinitrak.auth.aspect.AuditAspect}.
 * Les logs d'audit sont en écriture seule (aucune mise à jour possible).
 */
@Entity
@Table(name = "audit_logs", indexes = {
    @Index(name = "idx_audit_user", columnList = "user_id"),
    @Index(name = "idx_audit_tenant", columnList = "tenant_id"),
    @Index(name = "idx_audit_created_at", columnList = "created_at"),
    @Index(name = "idx_audit_action", columnList = "action")
})
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AuditLog {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", updatable = false, nullable = false)
    private UUID id;

    /** Identifiant de l'utilisateur ayant déclenché l'action. */
    @Column(name = "user_id")
    private UUID userId;

    /** Email de l'utilisateur au moment de l'action (dénormalisé pour lecture). */
    @Column(name = "user_email", length = 255)
    private String userEmail;

    /** Tenant dans lequel l'action a eu lieu. */
    @Column(name = "tenant_id")
    private UUID tenantId;

    /** Action effectuée (ex: "AUTH_LOGIN", "USER_CREATE"). */
    @Column(name = "action", nullable = false, length = 100)
    private String action;

    /** Ressource ciblée (ex: "User", "Study"). */
    @Column(name = "resource_type", length = 100)
    private String resourceType;

    /** Identifiant de la ressource ciblée. */
    @Column(name = "resource_id", length = 255)
    private String resourceId;

    /** Détails supplémentaires en JSON. */
    @Column(name = "details", columnDefinition = "jsonb")
    private String details;

    /** Adresse IP source. */
    @Column(name = "ip_address", length = 45)
    private String ipAddress;

    /** User-Agent de la requête. */
    @Column(name = "user_agent", length = 512)
    private String userAgent;

    /** Code de statut HTTP de la réponse. */
    @Column(name = "http_status")
    private Integer httpStatus;

    /** Timestamp de l'action. */
    @Column(name = "created_at", nullable = false, updatable = false)
    @Builder.Default
    private Instant createdAt = Instant.now();

    /** Résultat : SUCCESS ou FAILURE. */
    @Enumerated(EnumType.STRING)
    @Column(name = "outcome", nullable = false, length = 20)
    @Builder.Default
    private Outcome outcome = Outcome.SUCCESS;

    public enum Outcome {
        SUCCESS, FAILURE
    }
}
