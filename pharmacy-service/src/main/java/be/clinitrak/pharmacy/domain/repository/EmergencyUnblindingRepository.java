package be.clinitrak.pharmacy.domain.repository;

import be.clinitrak.pharmacy.domain.entity.EmergencyUnblinding;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

/**
 * Repository JPA pour les levées d'insu d'urgence.
 *
 * <p>Toutes les requêtes filtrent sur {@code deleted = false} pour respecter
 * le principe de soft-delete. Le {@code tenantId} garantit l'isolation multi-tenant.
 */
@Repository
public interface EmergencyUnblindingRepository extends JpaRepository<EmergencyUnblinding, UUID> {

    /**
     * Retourne toutes les levées d'insu non supprimées d'un tenant.
     *
     * @param tenantId identifiant du tenant
     * @return liste des levées d'insu actives
     */
    List<EmergencyUnblinding> findByTenantIdAndDeletedFalse(String tenantId);

    /**
     * Retourne les levées d'insu d'une étude pour un tenant.
     *
     * @param studyId  identifiant de l'étude
     * @param tenantId identifiant du tenant
     * @return liste des levées d'insu de l'étude
     */
    List<EmergencyUnblinding> findByStudyIdAndTenantIdAndDeletedFalse(String studyId, String tenantId);
}
