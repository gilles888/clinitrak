package be.clinitrak.ethics.domain.repository;

import be.clinitrak.ethics.domain.entity.CorrespondenceTemplate;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Repository pour les modèles de correspondance.
 *
 * <p>Les templates globaux (tenantId = null) sont accessibles à tous les tenants.
 * Les templates spécifiques à un tenant ont priorité sur les templates globaux.
 */
@Repository
public interface CorrespondenceTemplateRepository extends JpaRepository<CorrespondenceTemplate, UUID> {

    /**
     * Retourne les templates actifs disponibles pour un tenant (incluant les templates globaux).
     *
     * @param tenantId identifiant du tenant
     * @return liste des templates actifs (globaux + spécifiques au tenant)
     */
    @Query("SELECT t FROM CorrespondenceTemplate t WHERE t.isActive = true AND t.deleted = false " +
           "AND (t.tenantId IS NULL OR t.tenantId = :tenantId)")
    List<CorrespondenceTemplate> findActiveTemplatesForTenant(@Param("tenantId") UUID tenantId);

    /**
     * Recherche un template actif par son code.
     *
     * @param templateCode code unique du template
     * @return template trouvé ou empty
     */
    Optional<CorrespondenceTemplate> findByTemplateCodeAndIsActiveTrueAndDeletedFalse(String templateCode);

    /**
     * Recherche un template actif par son code (version simplifiée sans deleted check,
     * compatible avec les templates seedés sans le flag).
     *
     * @param code code unique du template
     * @return template trouvé ou empty
     */
    Optional<CorrespondenceTemplate> findByTemplateCodeAndIsActiveTrue(String code);

    /**
     * Retourne tous les templates actifs (pour les cas où le tenant n'est pas connu).
     *
     * @return liste des templates actifs
     */
    List<CorrespondenceTemplate> findByIsActiveTrueAndDeletedFalse();
}
