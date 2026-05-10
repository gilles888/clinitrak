package be.clinitrak.ctc.domain.repository;

import be.clinitrak.ctc.domain.entity.TrialDeskRequest;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

/**
 * Repository JPA pour les demandes desk CTC.
 *
 * <p>Toutes les méthodes de requête filtrent sur {@code deleted = false}
 * pour respecter la convention de soft-delete du projet.
 */
@Repository
public interface TrialDeskRequestRepository extends JpaRepository<TrialDeskRequest, UUID> {

    /**
     * Retourne toutes les demandes actives d'un tenant.
     *
     * @param tenantId identifiant du tenant
     * @return liste des demandes non supprimées du tenant
     */
    List<TrialDeskRequest> findByTenantIdAndDeletedFalse(String tenantId);

    /**
     * Retourne les demandes actives d'une étude pour un tenant donné.
     *
     * @param studyId  identifiant de l'étude (référence study-service)
     * @param tenantId identifiant du tenant
     * @return liste des demandes de cette étude pour ce tenant
     */
    List<TrialDeskRequest> findByStudyIdAndTenantIdAndDeletedFalse(String studyId, String tenantId);
}
