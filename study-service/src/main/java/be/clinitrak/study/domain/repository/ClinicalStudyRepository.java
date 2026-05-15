package be.clinitrak.study.domain.repository;

import be.clinitrak.study.domain.entity.ClinicalStudy;
import be.clinitrak.study.domain.enums.StudyStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Map;
import java.util.Optional;
import java.util.UUID;

/**
 * Repository JPA pour les études cliniques.
 *
 * <p>Étend {@link JpaSpecificationExecutor} pour supporter les recherches multicritères
 * via les Specifications JPA (pattern Criteria API).
 */
@Repository
public interface ClinicalStudyRepository extends JpaRepository<ClinicalStudy, UUID>, JpaSpecificationExecutor<ClinicalStudy> {

    /**
     * Recherche une étude par son identifiant et tenant, en excluant les supprimées.
     *
     * @param id       identifiant de l'étude
     * @param tenantId identifiant du tenant
     * @return étude trouvée ou vide si non trouvée ou supprimée
     */
    Optional<ClinicalStudy> findByIdAndTenantIdAndDeletedFalse(UUID id, UUID tenantId);

    /**
     * Vérifie l'existence d'un numéro d'étude au sein d'un tenant.
     *
     * @param studyNumber numéro d'étude à vérifier
     * @param tenantId    identifiant du tenant
     * @return true si le numéro existe déjà
     */
    boolean existsByStudyNumberAndTenantId(String studyNumber, UUID tenantId);

    /**
     * Vérifie l'existence d'un numéro d'éthique au sein d'un tenant.
     *
     * @param ethicsNumber numéro éthique à vérifier
     * @param tenantId     identifiant du tenant
     * @return true si le numéro existe déjà
     */
    boolean existsByEthicsNumberAndTenantId(String ethicsNumber, UUID tenantId);

    /**
     * Compte les études actives (non supprimées) par statut pour un tenant donné.
     *
     * @param tenantId identifiant du tenant
     * @param status   statut à compter
     * @return nombre d'études
     */
    long countByTenantIdAndCurrentStatusAndDeletedFalse(UUID tenantId, StudyStatus status);

    /**
     * Compte toutes les études actives d'un tenant.
     *
     * @param tenantId identifiant du tenant
     * @return nombre total d'études non supprimées
     */
    long countByTenantIdAndDeletedFalse(UUID tenantId);

    /**
     * Compte les études dont les CUSL sont le promoteur.
     *
     * @param tenantId      identifiant du tenant
     * @param isSponsorCusl true pour les études CUSL
     * @return nombre d'études
     */
    long countByTenantIdAndIsSponsorCuslAndDeletedFalse(UUID tenantId, boolean isSponsorCusl);

    /**
     * Récupère le nombre d'études par domaine thérapeutique pour un tenant.
     *
     * @param tenantId identifiant du tenant
     * @return liste de paires [domaine, count]
     */
    @Query("SELECT c.therapeuticArea, COUNT(c) FROM ClinicalStudy c " +
           "WHERE c.tenantId = :tenantId AND c.deleted = false AND c.therapeuticArea IS NOT NULL " +
           "GROUP BY c.therapeuticArea ORDER BY COUNT(c) DESC")
    java.util.List<Object[]> countByTherapeuticAreaAndTenantId(@Param("tenantId") UUID tenantId);

    /**
     * Récupère le nombre d'études par phase pour un tenant.
     *
     * @param tenantId identifiant du tenant
     * @return liste de paires [phase, count]
     */
    @Query("SELECT c.phase, COUNT(c) FROM ClinicalStudy c " +
           "WHERE c.tenantId = :tenantId AND c.deleted = false AND c.phase IS NOT NULL " +
           "GROUP BY c.phase ORDER BY COUNT(c) DESC")
    java.util.List<Object[]> countByPhaseAndTenantId(@Param("tenantId") UUID tenantId);

    /**
     * Récupère le prochain numéro de séquence pour une année donnée et un tenant.
     *
     * @param tenantId identifiant du tenant
     * @param year     année au format YYYY
     * @return nombre d'études créées cette année pour ce tenant
     */
    @Query("SELECT COUNT(c) FROM ClinicalStudy c " +
           "WHERE c.tenantId = :tenantId AND EXTRACT(YEAR FROM c.createdAt) = :year")
    long countByTenantIdAndYear(@Param("tenantId") UUID tenantId, @Param("year") int year);
}
