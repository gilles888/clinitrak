package be.clinitrak.admin.domain.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Journal d'audit système de la plateforme CliniTrak.
 *
 * <p>Enregistre chaque action administrative significative (création de tenant,
 * modification de configuration, invitation d'utilisateur, etc.).
 *
 * <p><strong>Insert-only :</strong> les logs d'audit ne sont jamais supprimés ni modifiés
 * (pas de soft-delete, pas de version). C'est un journal immuable.
 */
@Getter
@Setter
@Entity
@Table(name = "admin_audit_logs")
@EntityListeners(AuditingEntityListener.class)
public class SystemAuditLog {

    /** Identifiant UUID généré automatiquement. */
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", updatable = false, nullable = false)
    private UUID id;

    /** Identifiant du tenant concerné par l'action. */
    @Column(name = "tenant_id", length = 255)
    private String tenantId;

    /** Identifiant de l'utilisateur ayant effectué l'action. */
    @Column(name = "user_id", length = 255)
    private String userId;

    /** Action effectuée (ex: CREATE_TENANT, UPDATE_CONFIG, INVITE_USER). */
    @Column(name = "action", nullable = false, length = 100)
    private String action;

    /** Type de l'entité concernée (ex: Tenant, User). */
    @Column(name = "entity_type", length = 100)
    private String entityType;

    /** Identifiant de l'entité concernée. */
    @Column(name = "entity_id", length = 255)
    private String entityId;

    /** Valeur avant modification (JSON sérialisé). */
    @Column(name = "old_value", columnDefinition = "TEXT")
    private String oldValue;

    /** Valeur après modification (JSON sérialisé). */
    @Column(name = "new_value", columnDefinition = "TEXT")
    private String newValue;

    /** Adresse IP du client ayant effectué l'action. */
    @Column(name = "ip_address", length = 50)
    private String ipAddress;

    /** User-Agent du client. */
    @Column(name = "user_agent", columnDefinition = "TEXT")
    private String userAgent;

    /** Timestamp de l'action (non nullable, jamais modifié). */
    @Column(name = "timestamp", nullable = false, updatable = false)
    private LocalDateTime timestamp;
}
