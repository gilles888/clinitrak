package be.clinitrak.ethics.domain.repository;

import be.clinitrak.ethics.domain.entity.Correspondence;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.UUID;

/**
 * Repository pour les correspondances générées par le Comité d'Éthique.
 */
@Repository
public interface CorrespondenceRepository extends JpaRepository<Correspondence, UUID> {

    /**
     * Retourne les correspondances d'une étude pour un tenant, triées du plus récent au plus ancien.
     *
     * @param studyId  identifiant de l'étude
     * @param tenantId identifiant du tenant
     * @param pageable paramètres de pagination
     * @return page de correspondances
     */
    Page<Correspondence> findByStudyIdAndTenantIdOrderByGeneratedDateDesc(
        UUID studyId,
        UUID tenantId,
        Pageable pageable
    );
}
