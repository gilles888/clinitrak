package be.clinitrak.ethics.domain.repository;

import be.clinitrak.ethics.domain.entity.Meeting;
import be.clinitrak.ethics.domain.enums.MeetingStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Repository pour les réunions du Comité d'Éthique.
 */
@Repository
public interface MeetingRepository extends JpaRepository<Meeting, UUID> {

    /**
     * Retourne les réunions d'un tenant dans une plage de dates.
     *
     * @param tenantId  identifiant du tenant
     * @param from      date de début (inclusive)
     * @param to        date de fin (inclusive)
     * @return liste des réunions triée par date croissante
     */
    List<Meeting> findByTenantIdAndMeetingDateBetweenOrderByMeetingDate(
        UUID tenantId,
        LocalDate from,
        LocalDate to
    );

    /**
     * Retourne les réunions planifiées après une date donnée.
     *
     * @param tenantId  identifiant du tenant
     * @param status    statut des réunions à retourner
     * @param after     date de référence (exclusive)
     * @return liste des réunions planifiées triée par date croissante
     */
    List<Meeting> findByTenantIdAndStatusAndMeetingDateAfterOrderByMeetingDate(
        UUID tenantId,
        MeetingStatus status,
        LocalDate after
    );

    /**
     * Retourne une réunion par son identifiant et son tenant.
     *
     * @param id        identifiant de la réunion
     * @param tenantId  identifiant du tenant
     * @return réunion trouvée ou empty
     */
    Optional<Meeting> findByIdAndTenantId(UUID id, UUID tenantId);

    /**
     * Compte les réunions planifiées après une date donnée pour un tenant.
     *
     * @param tenantId  identifiant du tenant
     * @param status    statut des réunions
     * @param after     date de référence
     * @return nombre de réunions correspondantes
     */
    long countByTenantIdAndStatusAndMeetingDateAfter(UUID tenantId, MeetingStatus status, LocalDate after);
}
