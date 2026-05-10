package be.clinitrak.study.domain.repository;

import be.clinitrak.study.domain.entity.Patient;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

/**
 * Repository JPA pour les patients pseudonymisés des études cliniques.
 */
@Repository
public interface PatientRepository extends JpaRepository<Patient, UUID> {

    /**
     * Retourne les patients d'une étude pour un tenant, avec pagination.
     *
     * @param studyId  identifiant de l'étude
     * @param tenantId identifiant du tenant
     * @param pageable paramètres de pagination et tri
     * @return page de patients
     */
    Page<Patient> findByStudyIdAndTenantId(UUID studyId, UUID tenantId, Pageable pageable);

    /**
     * Compte le nombre de patients dans une étude pour un tenant.
     *
     * @param studyId  identifiant de l'étude
     * @param tenantId identifiant du tenant
     * @return nombre de patients
     */
    long countByStudyIdAndTenantId(UUID studyId, UUID tenantId);

    /**
     * Vérifie si un code patient existe déjà dans une étude.
     *
     * @param patientCode code pseudonyme du patient
     * @param studyId     identifiant de l'étude
     * @return true si le code existe déjà
     */
    boolean existsByPatientCodeAndStudyId(String patientCode, UUID studyId);

    /**
     * Recherche un patient par son ID, l'étude et le tenant.
     *
     * @param id       identifiant du patient
     * @param studyId  identifiant de l'étude
     * @param tenantId identifiant du tenant
     * @return patient trouvé ou vide
     */
    Optional<Patient> findByIdAndStudyIdAndTenantId(UUID id, UUID studyId, UUID tenantId);
}
