package be.clinitrak.notification.domain.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Entité de base pour toutes les entités JPA du notification-service.
 * Fournit l'identifiant UUID, les timestamps d'audit, le soft-delete
 * et le mécanisme de verrouillage optimiste.
 */
@Getter
@Setter
@MappedSuperclass
@EntityListeners(AuditingEntityListener.class)
public abstract class BaseEntity {

    /** Identifiant unique UUID généré automatiquement. */
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", updatable = false, nullable = false)
    private UUID id;

    /** Date et heure de création de l'entité. */
    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    /** Date et heure de la dernière modification de l'entité. */
    @LastModifiedDate
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    /** Date et heure de suppression logique. Null si l'entité est active. */
    @Column(name = "deleted_at")
    private LocalDateTime deletedAt;

    /**
     * Numéro de version pour le verrouillage optimiste.
     */
    @Version
    @Column(name = "version_lock")
    private Long versionLock;

    /**
     * Indique si l'entité est supprimée logiquement.
     *
     * @return {@code true} si l'entité a été supprimée
     */
    public boolean isDeleted() {
        return deletedAt != null;
    }

    /**
     * Effectue une suppression logique.
     */
    public void softDelete() {
        this.deletedAt = LocalDateTime.now();
    }
}
