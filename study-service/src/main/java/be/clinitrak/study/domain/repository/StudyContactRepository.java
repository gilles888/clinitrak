package be.clinitrak.study.domain.repository;

import be.clinitrak.study.domain.entity.StudyContact;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Repository JPA pour les contacts d'études cliniques.
 */
@Repository
public interface StudyContactRepository extends JpaRepository<StudyContact, UUID> {

    /**
     * Retourne les contacts actifs d'une étude pour un tenant donné.
     *
     * @param studyId  identifiant de l'étude
     * @param tenantId identifiant du tenant
     * @return liste des contacts actifs
     */
    List<StudyContact> findByStudyIdAndTenantIdAndActiveTrue(UUID studyId, UUID tenantId);

    /**
     * Recherche un contact par son ID, l'étude et le tenant.
     *
     * @param id       identifiant du contact
     * @param studyId  identifiant de l'étude
     * @param tenantId identifiant du tenant
     * @return contact trouvé ou vide
     */
    Optional<StudyContact> findByIdAndStudyIdAndTenantId(UUID id, UUID studyId, UUID tenantId);
}
