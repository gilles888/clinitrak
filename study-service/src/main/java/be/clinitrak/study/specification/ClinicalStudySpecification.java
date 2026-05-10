package be.clinitrak.study.specification;

import be.clinitrak.study.domain.entity.ClinicalStudy;
import be.clinitrak.study.domain.enums.StudyPhase;
import be.clinitrak.study.domain.enums.StudyStatus;
import be.clinitrak.study.domain.enums.SponsorType;
import be.clinitrak.study.dto.StudySearchCriteria;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.util.StringUtils;

import java.time.LocalDate;
import java.util.UUID;

/**
 * Specifications JPA pour la recherche multicritères d'études cliniques.
 *
 * <p>Chaque méthode statique retourne une {@link Specification} qui peut être
 * combinée avec {@code Specification.where().and().and()...} pour construire
 * dynamiquement la requête en fonction des critères fournis.
 *
 * <p>Les critères null ou vides sont ignorés (pas de prédicat ajouté).
 */
public class ClinicalStudySpecification {

    private ClinicalStudySpecification() {}

    /**
     * Construit une Specification complète depuis les critères de recherche et le tenant.
     *
     * @param criteria critères de recherche (tous optionnels)
     * @param tenantId identifiant du tenant courant (obligatoire)
     * @return Specification combinée prête à être utilisée avec le repository
     */
    public static Specification<ClinicalStudy> buildFrom(StudySearchCriteria criteria, UUID tenantId) {
        return Specification.where(hasTenant(tenantId))
            .and(notDeleted())
            .and(hasEthicsNumber(criteria.ethicsNumber()))
            .and(hasEudractNumber(criteria.eudractNumber()))
            .and(hasCtisNumber(criteria.ctisNumber()))
            .and(hasAcronymLike(criteria.acronym()))
            .and(hasTitleLike(criteria.titleKeyword()))
            .and(hasSponsorType(criteria.sponsorType()))
            .and(hasStatus(criteria.status()))
            .and(hasTherapeuticArea(criteria.therapeuticArea()))
            .and(hasPhase(criteria.phase()))
            .and(startDateFrom(criteria.startDateFrom()))
            .and(startDateTo(criteria.startDateTo()))
            .and(isSponsorCusl(criteria.isSponsorCusl()));
    }

    /**
     * Filtre sur le tenant courant.
     *
     * @param tenantId identifiant du tenant
     * @return Specification de filtrage par tenant
     */
    private static Specification<ClinicalStudy> hasTenant(UUID tenantId) {
        return (root, query, cb) -> cb.equal(root.get("tenantId"), tenantId);
    }

    /**
     * Exclut les études marquées comme supprimées (soft delete).
     *
     * @return Specification excluant les entités supprimées
     */
    private static Specification<ClinicalStudy> notDeleted() {
        return (root, query, cb) -> cb.isFalse(root.get("deleted"));
    }

    /**
     * Filtre exact sur le numéro de Comité d'Éthique.
     *
     * @param ethicsNumber numéro CE ou null pour ignorer le critère
     * @return Specification ou null si critère vide
     */
    private static Specification<ClinicalStudy> hasEthicsNumber(String ethicsNumber) {
        if (!StringUtils.hasText(ethicsNumber)) {
            return null;
        }
        return (root, query, cb) -> cb.equal(root.get("ethicsNumber"), ethicsNumber.trim());
    }

    /**
     * Filtre exact sur le numéro EudraCT.
     *
     * @param eudractNumber numéro EudraCT ou null pour ignorer le critère
     * @return Specification ou null si critère vide
     */
    private static Specification<ClinicalStudy> hasEudractNumber(String eudractNumber) {
        if (!StringUtils.hasText(eudractNumber)) {
            return null;
        }
        return (root, query, cb) -> cb.equal(root.get("eudractNumber"), eudractNumber.trim());
    }

    /**
     * Filtre exact sur le numéro CTIS.
     *
     * @param ctisNumber numéro CTIS ou null pour ignorer le critère
     * @return Specification ou null si critère vide
     */
    private static Specification<ClinicalStudy> hasCtisNumber(String ctisNumber) {
        if (!StringUtils.hasText(ctisNumber)) {
            return null;
        }
        return (root, query, cb) -> cb.equal(root.get("ctisNumber"), ctisNumber.trim());
    }

    /**
     * Filtre partiel (LIKE insensible à la casse) sur l'acronyme.
     *
     * @param acronym acronyme partiel ou null pour ignorer le critère
     * @return Specification ou null si critère vide
     */
    private static Specification<ClinicalStudy> hasAcronymLike(String acronym) {
        if (!StringUtils.hasText(acronym)) {
            return null;
        }
        return (root, query, cb) -> cb.like(
            cb.lower(root.get("acronym")),
            "%" + acronym.trim().toLowerCase() + "%"
        );
    }

    /**
     * Filtre partiel (LIKE insensible à la casse) sur le titre.
     *
     * @param titleKeyword mot-clé dans le titre ou null pour ignorer le critère
     * @return Specification ou null si critère vide
     */
    private static Specification<ClinicalStudy> hasTitleLike(String titleKeyword) {
        if (!StringUtils.hasText(titleKeyword)) {
            return null;
        }
        return (root, query, cb) -> cb.like(
            cb.lower(root.get("title")),
            "%" + titleKeyword.trim().toLowerCase() + "%"
        );
    }

    /**
     * Filtre exact sur le type de promoteur.
     *
     * @param sponsorType type de promoteur ou null pour ignorer le critère
     * @return Specification ou null si critère null
     */
    private static Specification<ClinicalStudy> hasSponsorType(SponsorType sponsorType) {
        if (sponsorType == null) {
            return null;
        }
        return (root, query, cb) -> cb.equal(root.get("sponsorType"), sponsorType);
    }

    /**
     * Filtre exact sur le statut courant de l'étude.
     *
     * @param status statut de l'étude ou null pour ignorer le critère
     * @return Specification ou null si critère null
     */
    private static Specification<ClinicalStudy> hasStatus(StudyStatus status) {
        if (status == null) {
            return null;
        }
        return (root, query, cb) -> cb.equal(root.get("currentStatus"), status);
    }

    /**
     * Filtre exact sur le domaine thérapeutique.
     *
     * @param therapeuticArea domaine thérapeutique ou null pour ignorer le critère
     * @return Specification ou null si critère vide
     */
    private static Specification<ClinicalStudy> hasTherapeuticArea(String therapeuticArea) {
        if (!StringUtils.hasText(therapeuticArea)) {
            return null;
        }
        return (root, query, cb) -> cb.equal(root.get("therapeuticArea"), therapeuticArea.trim());
    }

    /**
     * Filtre exact sur la phase de l'essai clinique.
     *
     * @param phase phase de l'essai ou null pour ignorer le critère
     * @return Specification ou null si critère null
     */
    private static Specification<ClinicalStudy> hasPhase(StudyPhase phase) {
        if (phase == null) {
            return null;
        }
        return (root, query, cb) -> cb.equal(root.get("phase"), phase);
    }

    /**
     * Filtre sur la date de début minimale (incluse).
     *
     * @param from date de début minimale ou null pour ignorer le critère
     * @return Specification ou null si critère null
     */
    private static Specification<ClinicalStudy> startDateFrom(LocalDate from) {
        if (from == null) {
            return null;
        }
        return (root, query, cb) -> cb.greaterThanOrEqualTo(root.get("startDate"), from);
    }

    /**
     * Filtre sur la date de début maximale (incluse).
     *
     * @param to date de début maximale ou null pour ignorer le critère
     * @return Specification ou null si critère null
     */
    private static Specification<ClinicalStudy> startDateTo(LocalDate to) {
        if (to == null) {
            return null;
        }
        return (root, query, cb) -> cb.lessThanOrEqualTo(root.get("startDate"), to);
    }

    /**
     * Filtre sur le flag promoteur CUSL.
     *
     * @param cusl true/false ou null pour ignorer le critère
     * @return Specification ou null si critère null
     */
    private static Specification<ClinicalStudy> isSponsorCusl(Boolean cusl) {
        if (cusl == null) {
            return null;
        }
        return (root, query, cb) -> cb.equal(root.get("isSponsorCusl"), cusl);
    }
}
