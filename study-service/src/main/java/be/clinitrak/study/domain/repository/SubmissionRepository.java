package be.clinitrak.study.domain.repository;

import be.clinitrak.study.domain.entity.Submission;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

/**
 * Repository JPA pour les soumissions réglementaires.
 */
@Repository
public interface SubmissionRepository extends JpaRepository<Submission, UUID> {

    /**
     * Retourne les soumissions d'une étude pour un tenant, avec pagination.
     *
     * @param studyId  identifiant de l'étude
     * @param tenantId identifiant du tenant
     * @param pageable paramètres de pagination et tri
     * @return page de soumissions
     */
    Page<Submission> findByStudyIdAndTenantId(UUID studyId, UUID tenantId, Pageable pageable);

    /**
     * Recherche une soumission par son ID, l'étude et le tenant.
     *
     * @param id       identifiant de la soumission
     * @param studyId  identifiant de l'étude
     * @param tenantId identifiant du tenant
     * @return soumission trouvée ou vide
     */
    Optional<Submission> findByIdAndStudyIdAndTenantId(UUID id, UUID studyId, UUID tenantId);
}
