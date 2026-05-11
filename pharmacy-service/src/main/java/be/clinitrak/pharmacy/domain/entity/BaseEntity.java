package be.clinitrak.pharmacy.domain.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.springframework.data.annotation.CreatedBy;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedBy;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.Instant;
import java.util.UUID;

/**
 * Entité de base commune à toutes les entités JPA du pharmacy-service.
 *
 * <p>Fournit l'identifiant UUID, les timestamps d'audit et le tracking de l'auteur
 * via Spring Data JPA Auditing. Le soft-delete est géré via le flag {@code deleted}.
 */
@Getter
@Setter
@MappedSuperclass
@EntityListeners(AuditingEntityListener.class)
public abstract class BaseEntity {

    /** Identifiant UUID généré automatiquement. */
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", updatable = false, nullable = false)
    private UUID id;

    /** Timestamp de création, géré par Spring Data JPA Auditing. */
    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    /** Timestamp de dernière modification. */
    @LastModifiedDate
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    /** Identifiant (email) de l'utilisateur ayant créé l'entité. */
    @CreatedBy
    @Column(name = "created_by", updatable = false, length = 255)
    private String createdBy;

    /** Identifiant (email) de l'utilisateur ayant modifié l'entité en dernier. */
    @LastModifiedBy
    @Column(name = "last_modified_by", length = 255)
    private String lastModifiedBy;

    /** Flag de suppression logique (soft delete). */
    @Column(name = "deleted", nullable = false)
    private boolean deleted = false;

    /** Version pour l'optimistic locking (évite les conflits de mise à jour). */
    @Version
    @Column(name = "version")
    private Long version;
}
