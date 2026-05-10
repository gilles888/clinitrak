package be.clinitrak.ctc.domain.repository;

import be.clinitrak.ctc.domain.entity.StatisticsRequest;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

/**
 * Repository JPA pour les demandes d'analyses statistiques.
 *
 * <p>Toutes les méthodes de requête filtrent sur {@code deleted = false}
 * pour respecter la convention de soft-delete du projet.
 */
@Repository
public interface StatisticsRequestRepository extends JpaRepository<StatisticsRequest, UUID> {

    /**
     * Retourne toutes les demandes statistiques actives d'un tenant.
     *
     * @param tenantId identifiant du tenant
     * @return liste des demandes non supprimées du tenant
     */
    List<StatisticsRequest> findByTenantIdAndDeletedFalse(String tenantId);
}
