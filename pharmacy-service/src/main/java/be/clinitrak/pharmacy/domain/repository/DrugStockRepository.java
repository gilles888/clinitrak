package be.clinitrak.pharmacy.domain.repository;

import be.clinitrak.pharmacy.domain.entity.DrugStock;
import be.clinitrak.pharmacy.domain.enums.StockStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

/**
 * Repository JPA pour les stocks de médicaments expérimentaux.
 *
 * <p>Toutes les requêtes filtrent sur {@code deleted = false} pour respecter
 * le principe de soft-delete. Le {@code tenantId} garantit l'isolation multi-tenant.
 */
@Repository
public interface DrugStockRepository extends JpaRepository<DrugStock, UUID> {

    /**
     * Retourne tous les stocks non supprimés d'un tenant.
     *
     * @param tenantId identifiant du tenant
     * @return liste des stocks actifs
     */
    List<DrugStock> findByTenantIdAndDeletedFalse(String tenantId);

    /**
     * Retourne les stocks d'une étude pour un tenant donné.
     *
     * @param studyId  identifiant de l'étude
     * @param tenantId identifiant du tenant
     * @return liste des stocks de l'étude
     */
    List<DrugStock> findByStudyIdAndTenantIdAndDeletedFalse(String studyId, String tenantId);

    /**
     * Retourne les stocks ayant un statut particulier pour un tenant.
     *
     * @param tenantId identifiant du tenant
     * @param status   statut du stock recherché
     * @return liste des stocks avec ce statut
     */
    List<DrugStock> findByTenantIdAndStatusAndDeletedFalse(String tenantId, StockStatus status);

    /**
     * Retourne les stocks dont la date de péremption est antérieure à la date fournie.
     *
     * @param tenantId identifiant du tenant
     * @param date     date de référence pour le filtre de péremption
     * @return liste des stocks expirant avant cette date
     */
    List<DrugStock> findByTenantIdAndExpiryDateBeforeAndDeletedFalse(String tenantId, LocalDate date);

    /**
     * Retourne les stocks en état AVAILABLE dont la quantité est inférieure au seuil.
     *
     * @param tenantId  identifiant du tenant
     * @param threshold seuil minimal de quantité
     * @return liste des stocks en dessous du seuil
     */
    @Query("SELECT s FROM DrugStock s WHERE s.tenantId = :tenantId AND s.deleted = false AND s.quantity < :threshold AND s.status = 'AVAILABLE'")
    List<DrugStock> findLowStock(@Param("tenantId") String tenantId, @Param("threshold") int threshold);
}
