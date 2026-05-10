package be.clinitrak.auth.domain.repository;

import be.clinitrak.auth.domain.entity.AuditLog;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.UUID;

/** Repository JPA pour les logs d'audit (écriture seule en pratique). */
@Repository
public interface AuditLogRepository extends JpaRepository<AuditLog, UUID> {

    /**
     * Recherche paginée des logs d'un tenant.
     *
     * @param tenantId  tenant cible
     * @param pageable  paramètres de pagination
     * @return page de logs
     */
    Page<AuditLog> findByTenantIdOrderByCreatedAtDesc(UUID tenantId, Pageable pageable);

    /**
     * Recherche paginée des logs d'un utilisateur.
     *
     * @param userId   utilisateur cible
     * @param pageable paramètres de pagination
     * @return page de logs
     */
    Page<AuditLog> findByUserIdOrderByCreatedAtDesc(UUID userId, Pageable pageable);

    /**
     * Recherche des logs dans une plage temporelle pour un tenant.
     *
     * @param tenantId  tenant cible
     * @param from      début de la plage
     * @param to        fin de la plage
     * @param pageable  pagination
     * @return page de logs
     */
    Page<AuditLog> findByTenantIdAndCreatedAtBetweenOrderByCreatedAtDesc(
        UUID tenantId, Instant from, Instant to, Pageable pageable
    );
}
