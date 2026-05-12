package be.clinitrak.admin.domain.repository;

import be.clinitrak.admin.domain.entity.SystemAuditLog;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

/**
 * Repository Spring Data JPA pour les journaux d'audit système.
 *
 * <p>Supporte la pagination et les spécifications JPA pour les filtres avancés.
 */
@Repository
public interface SystemAuditLogRepository extends JpaRepository<SystemAuditLog, UUID>,
    JpaSpecificationExecutor<SystemAuditLog> {

    /**
     * Recherche les logs d'audit par tenant avec pagination.
     *
     * @param tenantId identifiant du tenant
     * @param pageable paramètres de pagination
     * @return page de logs
     */
    Page<SystemAuditLog> findByTenantIdOrderByTimestampDesc(String tenantId, Pageable pageable);

    /**
     * Recherche les logs par action dans une plage de dates.
     *
     * @param action action à filtrer
     * @param from   date de début
     * @param to     date de fin
     * @return liste des logs
     */
    List<SystemAuditLog> findByActionAndTimestampBetweenOrderByTimestampDesc(
        String action, LocalDateTime from, LocalDateTime to);
}
