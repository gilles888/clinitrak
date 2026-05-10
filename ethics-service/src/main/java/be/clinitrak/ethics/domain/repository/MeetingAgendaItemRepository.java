package be.clinitrak.ethics.domain.repository;

import be.clinitrak.ethics.domain.entity.MeetingAgendaItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

/**
 * Repository pour les items de l'ordre du jour des réunions CE.
 */
@Repository
public interface MeetingAgendaItemRepository extends JpaRepository<MeetingAgendaItem, UUID> {

    /**
     * Retourne les items d'une réunion pour un tenant, triés par ordre d'affichage.
     *
     * @param meetingId identifiant de la réunion
     * @param tenantId  identifiant du tenant
     * @return liste des items triée par {@code itemOrder} croissant
     */
    List<MeetingAgendaItem> findByMeetingIdAndTenantIdOrderByItemOrder(UUID meetingId, UUID tenantId);
}
