package be.clinitrak.ctc.domain.repository;

import be.clinitrak.ctc.domain.entity.SponsorCUSLStudy;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Repository JPA pour les études gérées par le CTC en tant que sponsor CUSL.
 *
 * <p>Toutes les méthodes de requête filtrent sur {@code deleted = false}
 * pour respecter la convention de soft-delete du projet.
 */
@Repository
public interface SponsorCUSLStudyRepository extends JpaRepository<SponsorCUSLStudy, UUID> {

    /**
     * Retourne toutes les études sponsor actives d'un tenant.
     *
     * @param tenantId identifiant du tenant
     * @return liste des études non supprimées du tenant
     */
    List<SponsorCUSLStudy> findByTenantIdAndDeletedFalse(String tenantId);

    /**
     * Retourne l'étude sponsor correspondant à un studyId pour un tenant donné.
     *
     * @param studyId  identifiant de l'étude (référence study-service, unique par tenant)
     * @param tenantId identifiant du tenant
     * @return optionnel de l'étude sponsor si elle existe
     */
    Optional<SponsorCUSLStudy> findByStudyIdAndTenantIdAndDeletedFalse(String studyId, String tenantId);
}
