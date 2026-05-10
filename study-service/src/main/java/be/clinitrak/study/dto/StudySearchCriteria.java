package be.clinitrak.study.dto;

import be.clinitrak.study.domain.enums.StudyPhase;
import be.clinitrak.study.domain.enums.StudyStatus;
import be.clinitrak.study.domain.enums.SponsorType;

import java.time.LocalDate;

/**
 * Critères de recherche multicritères pour les études cliniques.
 *
 * <p>Tous les champs sont optionnels (nullable). Les critères non renseignés
 * ne sont pas appliqués dans la requête de recherche.
 *
 * <p>Utilisé avec {@link be.clinitrak.study.specification.ClinicalStudySpecification}
 * pour construire dynamiquement la requête JPA Criteria.
 *
 * @param ethicsNumber     filtre exact sur le numéro de comité d'éthique
 * @param eudractNumber    filtre exact sur le numéro EudraCT
 * @param ctisNumber       filtre exact sur le numéro CTIS
 * @param acronym          filtre partiel (LIKE) sur l'acronyme
 * @param titleKeyword     filtre partiel (LIKE) sur le titre
 * @param sponsorType      filtre exact sur le type de promoteur
 * @param status           filtre exact sur le statut courant
 * @param therapeuticArea  filtre exact sur le domaine thérapeutique
 * @param phase            filtre exact sur la phase de l'étude
 * @param startDateFrom    date de début minimale (incluse)
 * @param startDateTo      date de début maximale (incluse)
 * @param isSponsorCusl    si non null, filtre sur le flag CUSL
 */
public record StudySearchCriteria(
    String ethicsNumber,
    String eudractNumber,
    String ctisNumber,
    String acronym,
    String titleKeyword,
    SponsorType sponsorType,
    StudyStatus status,
    String therapeuticArea,
    StudyPhase phase,
    LocalDate startDateFrom,
    LocalDate startDateTo,
    Boolean isSponsorCusl
) {
    /**
     * Constructeur compact pour validation des invariants.
     * Aucune validation requise ici, tous les champs sont optionnels.
     */
    public StudySearchCriteria {
        // Tous les champs sont optionnels, pas de validation nécessaire
    }
}
