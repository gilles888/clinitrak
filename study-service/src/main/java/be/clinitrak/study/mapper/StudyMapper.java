package be.clinitrak.study.mapper;

import be.clinitrak.study.domain.entity.ClinicalStudy;
import be.clinitrak.study.domain.entity.Patient;
import be.clinitrak.study.domain.entity.StudyContact;
import be.clinitrak.study.domain.entity.Submission;
import be.clinitrak.study.domain.enums.*;
import be.clinitrak.study.dto.*;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;

/**
 * Mapper MapStruct pour la conversion entre entités JPA et DTOs du study-service.
 *
 * <p>MapStruct génère l'implémentation à la compilation.
 * Le composant est enregistré comme bean Spring via {@code componentModel = "spring"}.
 */
@Mapper(componentModel = "spring")
public interface StudyMapper {

    /**
     * Convertit une étude clinique en réponse complète avec libellés des enums.
     *
     * @param study entité ClinicalStudy
     * @return DTO StudyResponse avec tous les champs
     */
    @Mapping(target = "studyTypeLabel", expression = "java(mapStudyTypeLabel(study.getStudyType()))")
    @Mapping(target = "sponsorTypeLabel", expression = "java(mapSponsorTypeLabel(study.getSponsorType()))")
    @Mapping(target = "phaseLabel", expression = "java(mapPhaseLabel(study.getPhase()))")
    @Mapping(target = "currentStatusLabel", expression = "java(mapStatusLabel(study.getCurrentStatus()))")
    StudyResponse toResponse(ClinicalStudy study);

    /**
     * Convertit une étude clinique en résumé pour les tableaux de liste.
     *
     * @param study entité ClinicalStudy
     * @return DTO StudySummaryResponse allégé
     */
    @Mapping(target = "currentStatusLabel", expression = "java(mapStatusLabel(study.getCurrentStatus()))")
    StudySummaryResponse toSummary(ClinicalStudy study);

    /**
     * Crée une entité ClinicalStudy depuis une requête de création.
     *
     * @param request DTO de création
     * @return entité ClinicalStudy non persistée
     */
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    @Mapping(target = "createdBy", ignore = true)
    @Mapping(target = "updatedBy", ignore = true)
    @Mapping(target = "deleted", ignore = true)
    @Mapping(target = "version", ignore = true)
    @Mapping(target = "tenantId", ignore = true)
    @Mapping(target = "studyNumber", ignore = true)
    @Mapping(target = "currentStatus", ignore = true)
    @Mapping(target = "currentEnrollment", ignore = true)
    @Mapping(target = "approvalDate", ignore = true)
    ClinicalStudy fromCreateRequest(StudyCreateRequest request);

    /**
     * Met à jour une entité ClinicalStudy existante depuis une requête de mise à jour.
     *
     * @param request DTO de mise à jour
     * @param study   entité à modifier (annotée @MappingTarget)
     */
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    @Mapping(target = "createdBy", ignore = true)
    @Mapping(target = "updatedBy", ignore = true)
    @Mapping(target = "deleted", ignore = true)
    @Mapping(target = "version", ignore = true)
    @Mapping(target = "tenantId", ignore = true)
    @Mapping(target = "studyNumber", ignore = true)
    @Mapping(target = "currentStatus", ignore = true)
    @Mapping(target = "currentEnrollment", ignore = true)
    @Mapping(target = "approvalDate", ignore = true)
    void updateFromRequest(StudyUpdateRequest request, @MappingTarget ClinicalStudy study);

    /**
     * Convertit un contact d'étude en DTO de réponse.
     *
     * @param contact entité StudyContact
     * @return DTO ContactResponse avec libellé
     */
    @Mapping(target = "contactTypeLabel", expression = "java(mapContactTypeLabel(contact.getContactType()))")
    ContactResponse toContactResponse(StudyContact contact);

    /**
     * Crée un contact depuis une requête de création.
     *
     * @param request DTO de création de contact
     * @return entité StudyContact non persistée
     */
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    @Mapping(target = "createdBy", ignore = true)
    @Mapping(target = "updatedBy", ignore = true)
    @Mapping(target = "deleted", ignore = true)
    @Mapping(target = "version", ignore = true)
    @Mapping(target = "study", ignore = true)
    @Mapping(target = "tenantId", ignore = true)
    @Mapping(target = "active", ignore = true)
    StudyContact fromContactRequest(ContactRequest request);

    /**
     * Convertit une soumission en DTO de réponse.
     *
     * @param submission entité Submission
     * @return DTO SubmissionResponse
     */
    SubmissionResponse toSubmissionResponse(Submission submission);

    /**
     * Crée une soumission depuis une requête de création.
     *
     * @param request DTO de création de soumission
     * @return entité Submission non persistée
     */
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    @Mapping(target = "createdBy", ignore = true)
    @Mapping(target = "updatedBy", ignore = true)
    @Mapping(target = "deleted", ignore = true)
    @Mapping(target = "version", ignore = true)
    @Mapping(target = "study", ignore = true)
    @Mapping(target = "tenantId", ignore = true)
    @Mapping(target = "status", ignore = true)
    @Mapping(target = "receivedDate", ignore = true)
    Submission fromSubmissionRequest(SubmissionRequest request);

    /**
     * Convertit un patient en DTO de réponse.
     *
     * @param patient entité Patient
     * @return DTO PatientResponse
     */
    PatientResponse toPatientResponse(Patient patient);

    /**
     * Crée un patient depuis une requête de création.
     *
     * @param request DTO de création de patient
     * @return entité Patient non persistée
     */
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    @Mapping(target = "createdBy", ignore = true)
    @Mapping(target = "updatedBy", ignore = true)
    @Mapping(target = "deleted", ignore = true)
    @Mapping(target = "version", ignore = true)
    @Mapping(target = "study", ignore = true)
    @Mapping(target = "tenantId", ignore = true)
    @Mapping(target = "exclusionDate", ignore = true)
    Patient fromPatientRequest(PatientRequest request);

    // ----------------------------------------------------------------
    // Méthodes par défaut pour mapper les libellés des enums
    // ----------------------------------------------------------------

    /**
     * Retourne le libellé français du type d'étude.
     *
     * @param t type d'étude ou null
     * @return libellé ou null
     */
    default String mapStudyTypeLabel(StudyType t) {
        return t != null ? t.getLabel() : null;
    }

    /**
     * Retourne le libellé français du statut de l'étude.
     *
     * @param s statut ou null
     * @return libellé ou null
     */
    default String mapStatusLabel(StudyStatus s) {
        return s != null ? s.getLabel() : null;
    }

    /**
     * Retourne le libellé de la phase de l'essai.
     *
     * @param p phase ou null
     * @return libellé ou null
     */
    default String mapPhaseLabel(StudyPhase p) {
        return p != null ? p.getLabel() : null;
    }

    /**
     * Retourne le libellé français du type de promoteur.
     *
     * @param s type de promoteur ou null
     * @return libellé ou null
     */
    default String mapSponsorTypeLabel(SponsorType s) {
        return s != null ? s.getLabel() : null;
    }

    /**
     * Retourne le libellé français du type de contact.
     *
     * @param c type de contact ou null
     * @return libellé ou null
     */
    default String mapContactTypeLabel(ContactType c) {
        return c != null ? c.getLabel() : null;
    }
}
