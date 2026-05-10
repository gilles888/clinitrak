package be.clinitrak.ctc.domain.repository;

import be.clinitrak.ctc.domain.entity.FinancialContract;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

/**
 * Repository JPA pour les contrats financiers.
 *
 * <p>Toutes les méthodes de requête filtrent sur {@code deleted = false}
 * pour respecter la convention de soft-delete du projet.
 */
@Repository
public interface FinancialContractRepository extends JpaRepository<FinancialContract, UUID> {

    /**
     * Retourne tous les contrats actifs d'un tenant.
     *
     * @param tenantId identifiant du tenant
     * @return liste des contrats non supprimés du tenant
     */
    List<FinancialContract> findByTenantIdAndDeletedFalse(String tenantId);

    /**
     * Retourne les contrats actifs d'une étude pour un tenant donné.
     *
     * @param studyId  identifiant de l'étude (référence study-service)
     * @param tenantId identifiant du tenant
     * @return liste des contrats de cette étude pour ce tenant
     */
    List<FinancialContract> findByStudyIdAndTenantIdAndDeletedFalse(String studyId, String tenantId);
}
