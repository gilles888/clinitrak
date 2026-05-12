package be.clinitrak.notification.domain.repository;

import be.clinitrak.notification.domain.entity.Notification;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

/**
 * Repository JPA pour l'entité {@link Notification}.
 */
@Repository
public interface NotificationRepository extends JpaRepository<Notification, UUID> {

    /**
     * Recherche paginée des notifications actives d'un utilisateur, triées par date décroissante.
     *
     * @param userId   identifiant de l'utilisateur
     * @param pageable paramètres de pagination
     * @return page de notifications
     */
    @Query("""
            SELECT n FROM Notification n
            WHERE n.recipientUserId = :userId
            AND n.deletedAt IS NULL
            ORDER BY n.createdAt DESC
            """)
    Page<Notification> findByRecipientUserId(@Param("userId") UUID userId, Pageable pageable);

    /**
     * Recherche une notification active par son identifiant.
     *
     * @param id identifiant de la notification
     * @return Optional contenant la notification si elle existe
     */
    @Query("SELECT n FROM Notification n WHERE n.id = :id AND n.deletedAt IS NULL")
    Optional<Notification> findActiveById(@Param("id") UUID id);

    /**
     * Compte les notifications non lues d'un utilisateur.
     *
     * @param userId identifiant de l'utilisateur
     * @return nombre de notifications non lues
     */
    long countByRecipientUserIdAndReadFalseAndDeletedAtIsNull(UUID userId);
}
