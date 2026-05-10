package be.clinitrak.ctc.domain.repository;

import be.clinitrak.ctc.domain.entity.QualityEvent;
import be.clinitrak.ctc.domain.enums.EventStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

/**
 * Repository JPA pour les événements qualité.
 *
 * <p>Toutes les méthodes de requête filtrent sur {@code deleted = false}
 * pour respecter la convention de soft-delete du projet.
 */
@Repository
public interface QualityEventRepository extends JpaRepository<QualityEvent, UUID> {

    /**
     * Retourne tous les événements qualité actifs d'un tenant.
     *
     * @param tenantId identifiant du tenant
     * @return liste des événements non supprimés du tenant
     */
    List<QualityEvent> findByTenantIdAndDeletedFalse(String tenantId);

    /**
     * Retourne les événements qualité actifs d'une étude pour un tenant donné.
     *
     * @param studyId  identifiant de l'étude (référence study-service)
     * @param tenantId identifiant du tenant
     * @return liste des événements de cette étude pour ce tenant
     */
    List<QualityEvent> findByStudyIdAndTenantIdAndDeletedFalse(String studyId, String tenantId);

    /**
     * Retourne les événements qualité actifs d'un statut donné pour un tenant.
     *
     * @param tenantId identifiant du tenant
     * @param status   statut à filtrer
     * @return liste des événements dans ce statut pour ce tenant
     */
    List<QualityEvent> findByTenantIdAndStatusAndDeletedFalse(String tenantId, EventStatus status);
}
