package be.clinitrak.ctc.domain.repository;

import be.clinitrak.ctc.domain.entity.MonitoringVisit;
import be.clinitrak.ctc.domain.enums.VisitStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

/**
 * Repository JPA pour les visites de monitoring.
 *
 * <p>Toutes les méthodes de requête filtrent sur {@code deleted = false}
 * pour respecter la convention de soft-delete du projet.
 */
@Repository
public interface MonitoringVisitRepository extends JpaRepository<MonitoringVisit, UUID> {

    /**
     * Retourne toutes les visites actives d'un tenant.
     *
     * @param tenantId identifiant du tenant
     * @return liste des visites non supprimées du tenant
     */
    List<MonitoringVisit> findByTenantIdAndDeletedFalse(String tenantId);

    /**
     * Retourne les visites actives d'une étude pour un tenant donné.
     *
     * @param studyId  identifiant de l'étude (référence study-service)
     * @param tenantId identifiant du tenant
     * @return liste des visites de cette étude pour ce tenant
     */
    List<MonitoringVisit> findByStudyIdAndTenantIdAndDeletedFalse(String studyId, String tenantId);

    /**
     * Retourne les visites actives d'un statut donné pour un tenant.
     *
     * @param tenantId identifiant du tenant
     * @param status   statut à filtrer
     * @return liste des visites dans ce statut pour ce tenant
     */
    List<MonitoringVisit> findByTenantIdAndStatusAndDeletedFalse(String tenantId, VisitStatus status);
}
