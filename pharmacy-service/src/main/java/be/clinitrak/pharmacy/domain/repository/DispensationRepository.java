package be.clinitrak.pharmacy.domain.repository;

import be.clinitrak.pharmacy.domain.entity.Dispensation;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

/**
 * Repository JPA pour les dispensations de médicaments.
 *
 * <p>Toutes les requêtes filtrent sur {@code deleted = false} pour respecter
 * le principe de soft-delete. Le {@code tenantId} garantit l'isolation multi-tenant.
 */
@Repository
public interface DispensationRepository extends JpaRepository<Dispensation, UUID> {

    /**
     * Retourne toutes les dispensations non supprimées d'un tenant.
     *
     * @param tenantId identifiant du tenant
     * @return liste des dispensations actives
     */
    List<Dispensation> findByTenantIdAndDeletedFalse(String tenantId);

    /**
     * Retourne l'historique de dispensation d'un patient pour un tenant.
     *
     * @param patientCode code anonymisé du patient
     * @param tenantId    identifiant du tenant
     * @return liste des dispensations du patient
     */
    List<Dispensation> findByPatientCodeAndTenantIdAndDeletedFalse(String patientCode, String tenantId);

    /**
     * Retourne les dispensations d'une étude pour un tenant.
     *
     * @param studyId  identifiant de l'étude
     * @param tenantId identifiant du tenant
     * @return liste des dispensations de l'étude
     */
    List<Dispensation> findByStudyIdAndTenantIdAndDeletedFalse(String studyId, String tenantId);

    /**
     * Compte les dispensations effectuées à une date donnée pour un tenant.
     *
     * <p>Utilisé par le tableau de bord pour afficher le nombre de dispensations du jour.
     *
     * @param tenantId         identifiant du tenant
     * @param dispensationDate date de référence (typiquement {@code LocalDate.now()})
     * @return nombre de dispensations effectuées à cette date
     */
    @Query("SELECT COUNT(d) FROM Dispensation d WHERE d.tenantId = :tenantId AND d.dispensationDate = :dispensationDate AND d.deleted = false")
    long countByTenantIdAndDispensationDate(
        @Param("tenantId") String tenantId,
        @Param("dispensationDate") LocalDate dispensationDate
    );
}
