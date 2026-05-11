package be.clinitrak.pharmacy.domain.repository;

import be.clinitrak.pharmacy.domain.entity.PharmacyBilling;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

/**
 * Repository JPA pour la facturation pharmacie.
 *
 * <p>Toutes les requêtes filtrent sur {@code deleted = false} pour respecter
 * le principe de soft-delete. Le {@code tenantId} garantit l'isolation multi-tenant.
 */
@Repository
public interface PharmacyBillingRepository extends JpaRepository<PharmacyBilling, UUID> {

    /**
     * Retourne toutes les factures non supprimées d'un tenant.
     *
     * @param tenantId identifiant du tenant
     * @return liste des factures actives
     */
    List<PharmacyBilling> findByTenantIdAndDeletedFalse(String tenantId);
}
