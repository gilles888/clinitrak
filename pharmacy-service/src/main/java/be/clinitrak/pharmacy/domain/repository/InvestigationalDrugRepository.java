package be.clinitrak.pharmacy.domain.repository;

import be.clinitrak.pharmacy.domain.entity.InvestigationalDrug;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

/**
 * Repository JPA pour les médicaments expérimentaux.
 *
 * <p>Toutes les requêtes filtrent sur {@code deleted = false} pour respecter
 * le principe de soft-delete. Le {@code tenantId} garantit l'isolation multi-tenant.
 */
@Repository
public interface InvestigationalDrugRepository extends JpaRepository<InvestigationalDrug, UUID> {

    /**
     * Retourne tous les médicaments non supprimés d'un tenant.
     *
     * @param tenantId identifiant du tenant
     * @return liste des médicaments actifs
     */
    List<InvestigationalDrug> findByTenantIdAndDeletedFalse(String tenantId);

    /**
     * Retourne les médicaments d'une étude pour un tenant donné.
     *
     * @param studyId  identifiant de l'étude
     * @param tenantId identifiant du tenant
     * @return liste des médicaments de l'étude
     */
    List<InvestigationalDrug> findByStudyIdAndTenantIdAndDeletedFalse(String studyId, String tenantId);

    /**
     * Retourne les médicaments dont la date de péremption est antérieure à la date fournie.
     *
     * @param tenantId identifiant du tenant
     * @param date     date de référence pour le filtre de péremption
     * @return liste des médicaments expirant avant cette date
     */
    List<InvestigationalDrug> findByTenantIdAndExpiryDateBeforeAndDeletedFalse(String tenantId, LocalDate date);
}
