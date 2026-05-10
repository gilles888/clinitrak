package be.clinitrak.ethics.domain.repository;

import be.clinitrak.ethics.domain.entity.AnnualReport;
import be.clinitrak.ethics.domain.enums.AnnualReportStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

/**
 * Repository pour les rapports annuels attendus par le Comité d'Éthique.
 */
@Repository
public interface AnnualReportRepository extends JpaRepository<AnnualReport, UUID> {

    /**
     * Retourne les rapports d'un tenant paginés, triés par date d'échéance croissante.
     *
     * @param tenantId identifiant du tenant
     * @param pageable paramètres de pagination
     * @return page de rapports
     */
    Page<AnnualReport> findByTenantIdOrderByDueDateAsc(UUID tenantId, Pageable pageable);

    /**
     * Retourne les rapports en attente ou en retard dont la date d'échéance est avant {@code dueBefore}.
     *
     * @param tenantId  identifiant du tenant
     * @param dueBefore date limite
     * @return liste des rapports dus
     */
    @Query("SELECT ar FROM AnnualReport ar WHERE ar.tenantId = :tenantId " +
           "AND ar.status IN ('PENDING', 'OVERDUE') AND ar.dueDate <= :dueBefore AND ar.deleted = false")
    List<AnnualReport> findDueReportsByTenant(
        @Param("tenantId") UUID tenantId,
        @Param("dueBefore") LocalDate dueBefore
    );

    /**
     * Retourne les rapports en attente dont la date d'échéance est dans 60 jours
     * et dont le rappel J-60 n'a pas encore été envoyé.
     *
     * @param status   statut du rapport (PENDING)
     * @param dueDate  date d'échéance limite
     * @return liste des rapports éligibles au rappel J-60
     */
    List<AnnualReport> findByStatusAndDueDateBeforeAndReminderSent60False(
        AnnualReportStatus status,
        LocalDate dueDate
    );

    /**
     * Retourne les rapports en attente dont la date d'échéance est dans 30 jours
     * et dont le rappel J-30 n'a pas encore été envoyé.
     *
     * @param status   statut du rapport (PENDING)
     * @param dueDate  date d'échéance limite
     * @return liste des rapports éligibles au rappel J-30
     */
    List<AnnualReport> findByStatusAndDueDateBeforeAndReminderSent30False(
        AnnualReportStatus status,
        LocalDate dueDate
    );

    /**
     * Retourne les rapports en retard dont le rappel J-0 n'a pas encore été envoyé.
     *
     * @param status statut OVERDUE
     * @return liste des rapports éligibles au rappel J-0
     */
    List<AnnualReport> findByStatusAndReminderSent0False(AnnualReportStatus status);

    /**
     * Vérifie si un rapport existe déjà pour une étude, une année et un tenant donnés.
     *
     * @param studyId    identifiant de l'étude
     * @param reportYear année du rapport
     * @param tenantId   identifiant du tenant
     * @return true si le rapport existe déjà
     */
    boolean existsByStudyIdAndReportYearAndTenantId(UUID studyId, int reportYear, UUID tenantId);

    /**
     * Compte les rapports en retard pour un tenant.
     *
     * @param tenantId identifiant du tenant
     * @return nombre de rapports en retard
     */
    @Query("SELECT COUNT(ar) FROM AnnualReport ar WHERE ar.tenantId = :tenantId AND ar.status = 'OVERDUE' AND ar.deleted = false")
    long countOverdueByTenantId(@Param("tenantId") UUID tenantId);

    /**
     * Compte les rapports dont la date d'échéance est dans les X prochains jours.
     *
     * @param tenantId   identifiant du tenant
     * @param dueBefore  date limite
     * @return nombre de rapports dus prochainement
     */
    @Query("SELECT COUNT(ar) FROM AnnualReport ar WHERE ar.tenantId = :tenantId " +
           "AND ar.status IN ('PENDING', 'OVERDUE') AND ar.dueDate <= :dueBefore AND ar.deleted = false")
    long countDueSoonByTenantId(@Param("tenantId") UUID tenantId, @Param("dueBefore") LocalDate dueBefore);
}
