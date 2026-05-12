package be.clinitrak.admin.service;

import be.clinitrak.admin.domain.entity.SystemAuditLog;
import be.clinitrak.admin.domain.repository.SystemAuditLogRepository;
import be.clinitrak.admin.dto.AuditLogResponse;
import be.clinitrak.admin.dto.SystemHealthResponse;
import jakarta.persistence.EntityManager;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Service métier pour les journaux d'audit système et la santé de la plateforme.
 *
 * <p>Responsabilités :
 * <ul>
 *   <li>Recherche paginée des logs d'audit avec filtres multiples via JPA Specification</li>
 *   <li>Vérification de la connectivité à la base de données</li>
 *   <li>Rapport de santé global de la plateforme</li>
 * </ul>
 */
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class SystemAuditLogService {

    private final SystemAuditLogRepository auditLogRepository;
    private final EntityManager entityManager;

    /**
     * Recherche paginée des logs d'audit avec filtres optionnels.
     *
     * <p>Tous les filtres sont cumulatifs (AND). Un filtre {@code null} est ignoré.
     *
     * @param tenantId identifiant du tenant à filtrer (null = tous)
     * @param userId   identifiant de l'utilisateur à filtrer (null = tous)
     * @param action   action à filtrer (null = toutes)
     * @param from     date de début (null = pas de borne inférieure)
     * @param to       date de fin (null = pas de borne supérieure)
     * @param pageable paramètres de pagination et tri
     * @return page de logs d'audit correspondant aux critères
     */
    public Page<AuditLogResponse> findAll(
        String tenantId,
        String userId,
        String action,
        LocalDateTime from,
        LocalDateTime to,
        Pageable pageable
    ) {
        Specification<SystemAuditLog> spec = buildSpecification(tenantId, userId, action, from, to);
        return auditLogRepository.findAll(spec, pageable)
            .map(this::toResponse);
    }

    /**
     * Vérifie la connectivité à la base de données et retourne le rapport de santé.
     *
     * <p>Tente une requête SQL simple pour valider la connexion. Les autres services
     * (auth, study, etc.) sont reportés avec un statut indicatif basé sur la disponibilité
     * de la base partagée.
     *
     * @return {@link SystemHealthResponse} avec le statut "UP" ou "DEGRADED"
     */
    public SystemHealthResponse getSystemHealth() {
        Map<String, String> services = new LinkedHashMap<>();
        String overallStatus;

        boolean dbOk = checkDatabaseConnectivity();
        String dbStatus = dbOk ? "UP" : "DOWN";
        services.put("database", dbStatus);

        // Statuts indicatifs des services — dans l'architecture finale, ces
        // statuts seront obtenus via les endpoints /actuator/health de chaque service.
        services.put("auth-service", dbOk ? "UP" : "UNKNOWN");
        services.put("study-service", dbOk ? "UP" : "UNKNOWN");
        services.put("ethics-service", dbOk ? "UP" : "UNKNOWN");
        services.put("ctc-service", dbOk ? "UP" : "UNKNOWN");
        services.put("pharmacy-service", dbOk ? "UP" : "UNKNOWN");
        services.put("exchange-service", dbOk ? "UP" : "UNKNOWN");
        services.put("admin-service", "UP");

        overallStatus = dbOk ? "UP" : "DEGRADED";

        log.debug("Health check : statut global={}", overallStatus);

        return new SystemHealthResponse(overallStatus, services, LocalDateTime.now());
    }

    // ===== Méthodes privées =====

    /**
     * Construit une spécification JPA combinant tous les filtres fournis.
     *
     * @param tenantId identifiant du tenant (null = ignoré)
     * @param userId   identifiant de l'utilisateur (null = ignoré)
     * @param action   action (null = ignorée)
     * @param from     date de début (null = ignorée)
     * @param to       date de fin (null = ignorée)
     * @return spécification composite (AND de tous les filtres non null)
     */
    private Specification<SystemAuditLog> buildSpecification(
        String tenantId, String userId, String action,
        LocalDateTime from, LocalDateTime to
    ) {
        Specification<SystemAuditLog> spec = Specification.where(null);

        if (tenantId != null && !tenantId.isBlank()) {
            spec = spec.and((root, query, cb) ->
                cb.equal(root.get("tenantId"), tenantId));
        }

        if (userId != null && !userId.isBlank()) {
            spec = spec.and((root, query, cb) ->
                cb.equal(root.get("userId"), userId));
        }

        if (action != null && !action.isBlank()) {
            spec = spec.and((root, query, cb) ->
                cb.equal(root.get("action"), action));
        }

        if (from != null) {
            spec = spec.and((root, query, cb) ->
                cb.greaterThanOrEqualTo(root.get("timestamp"), from));
        }

        if (to != null) {
            spec = spec.and((root, query, cb) ->
                cb.lessThanOrEqualTo(root.get("timestamp"), to));
        }

        return spec;
    }

    /**
     * Vérifie la connectivité à la base de données via une requête SQL simple.
     *
     * @return {@code true} si la base est accessible, {@code false} sinon
     */
    private boolean checkDatabaseConnectivity() {
        try {
            entityManager.createNativeQuery("SELECT 1").getSingleResult();
            return true;
        } catch (Exception e) {
            log.error("Erreur de connectivité à la base de données : {}", e.getMessage());
            return false;
        }
    }

    /**
     * Convertit une entité {@link SystemAuditLog} en DTO {@link AuditLogResponse}.
     *
     * @param logEntry entité à convertir
     * @return DTO de réponse
     */
    private AuditLogResponse toResponse(SystemAuditLog logEntry) {
        return new AuditLogResponse(
            logEntry.getId(),
            logEntry.getTenantId(),
            logEntry.getUserId(),
            logEntry.getAction(),
            logEntry.getEntityType(),
            logEntry.getEntityId(),
            logEntry.getOldValue(),
            logEntry.getNewValue(),
            logEntry.getIpAddress(),
            logEntry.getTimestamp()
        );
    }
}
