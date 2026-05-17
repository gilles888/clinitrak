package be.clinitrak.ethics.domain.repository;

import be.clinitrak.ethics.domain.entity.EthicsReview;
import be.clinitrak.ethics.domain.enums.ReviewDecision;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Repository pour les avis éthiques.
 *
 * <p>Implémente {@link JpaSpecificationExecutor} pour les recherches multicritères dynamiques.
 * Toutes les requêtes filtrent sur {@code deleted = false} pour respecter le soft-delete.
 */
@Repository
public interface EthicsReviewRepository extends JpaRepository<EthicsReview, UUID>, JpaSpecificationExecutor<EthicsReview> {

    /**
     * Retourne tous les avis non supprimés d'une étude pour un tenant.
     *
     * @param studyId  identifiant de l'étude
     * @param tenantId identifiant du tenant
     * @return liste des avis de l'étude
     */
    List<EthicsReview> findByStudyIdAndTenantIdAndDeletedFalse(UUID studyId, UUID tenantId);

    /**
     * Retourne les avis d'un tenant paginés, triés du plus récent au plus ancien.
     *
     * @param tenantId identifiant du tenant
     * @param pageable paramètres de pagination
     * @return page d'avis
     */
    Page<EthicsReview> findByTenantIdAndDeletedFalseOrderBySubmissionDateDesc(UUID tenantId, Pageable pageable);

    /**
     * Retourne un avis par son identifiant et tenant (avec vérification soft-delete).
     *
     * @param id       identifiant de l'avis
     * @param tenantId identifiant du tenant
     * @return avis trouvé ou empty
     */
    Optional<EthicsReview> findByIdAndTenantIdAndDeletedFalse(UUID id, UUID tenantId);

    /**
     * Compte les avis par décision pour un tenant.
     *
     * @param tenantId identifiant du tenant
     * @param decision décision à compter
     * @return nombre d'avis pour cette décision
     */
    @Query("SELECT COUNT(r) FROM EthicsReview r WHERE r.tenantId = :tenantId AND r.decision = :decision AND r.deleted = false")
    long countByTenantIdAndDecision(
        @Param("tenantId") UUID tenantId,
        @Param("decision") ReviewDecision decision
    );

    /**
     * Retourne le nombre d'avis en attente pour un tenant.
     *
     * @param tenantId identifiant du tenant
     * @return nombre d'avis en attente
     */
    @Query("SELECT COUNT(r) FROM EthicsReview r WHERE r.tenantId = :tenantId AND r.decision = 'PENDING' AND r.deleted = false")
    long countPendingByTenantId(@Param("tenantId") UUID tenantId);

    /**
     * Retourne les avis soumis entre deux dates pour un tenant.
     *
     * @param tenantId identifiant du tenant
     * @param from     date de début (inclusive)
     * @param to       date de fin (inclusive)
     * @return nombre d'avis soumis dans cette période
     */
    @Query("SELECT COUNT(r) FROM EthicsReview r WHERE r.tenantId = :tenantId AND r.submissionDate BETWEEN :from AND :to AND r.deleted = false")
    long countByTenantIdAndSubmissionDateBetween(
        @Param("tenantId") UUID tenantId,
        @Param("from") LocalDate from,
        @Param("to") LocalDate to
    );

    /**
     * Retourne les 5 derniers avis en attente pour le tableau de bord.
     *
     * @param tenantId identifiant du tenant
     * @param pageable pageable avec limit=5
     * @return page des derniers avis en attente
     */
    @Query("SELECT r FROM EthicsReview r WHERE r.tenantId = :tenantId AND r.decision = 'PENDING' AND r.deleted = false ORDER BY r.submissionDate DESC")
    Page<EthicsReview> findRecentPendingByTenantId(@Param("tenantId") UUID tenantId, Pageable pageable);

    /**
     * Retourne tous les avis décidés avec leur décision pour les statistiques.
     *
     * @param tenantId identifiant du tenant
     * @return liste de paires [decision, count]
     */
    @Query("SELECT r.decision, COUNT(r) FROM EthicsReview r WHERE r.tenantId = :tenantId AND r.deleted = false GROUP BY r.decision")
    List<Object[]> countByDecisionAndTenantId(@Param("tenantId") UUID tenantId);

    /**
     * Calcule le délai moyen de traitement en jours entre la soumission et la décision CE.
     *
     * <p>Seuls les avis ayant une date de décision renseignée sont pris en compte.
     * Retourne {@code null} si aucun avis décidé n'est disponible pour ce tenant.
     * Utilise une requête native PostgreSQL pour calculer l'intervalle en jours.
     *
     * @param tenantId identifiant du tenant
     * @return délai moyen en jours, ou null si aucune donnée disponible
     */
    @Query(value = "SELECT AVG(EXTRACT(EPOCH FROM (decision_date::timestamp - submission_date::timestamp)) / 86400.0) " +
                   "FROM ethics_reviews " +
                   "WHERE tenant_id = :tenantId AND decision_date IS NOT NULL AND deleted = false",
           nativeQuery = true)
    Double computeAverageProcessingDays(@Param("tenantId") UUID tenantId);
}
