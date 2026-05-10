package be.clinitrak.study.dto;

import be.clinitrak.study.domain.enums.StudyPhase;
import be.clinitrak.study.domain.enums.StudyStatus;
import be.clinitrak.study.domain.enums.StudyType;
import be.clinitrak.study.domain.enums.SponsorType;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

/**
 * DTO de réponse complet pour une étude clinique.
 *
 * <p>Retourné par les endpoints GET /studies/{id}, POST /studies et PUT /studies/{id}.
 * Inclut tous les champs de l'étude ainsi que les libellés français des enums.
 *
 * @param id                   identifiant UUID de l'étude
 * @param tenantId             identifiant du tenant
 * @param studyNumber          numéro interne de l'étude (ST-YYYY-NNNNN)
 * @param ethicsNumber         numéro du Comité d'Éthique
 * @param eudractNumber        numéro EudraCT
 * @param ctisNumber           numéro CTIS EU
 * @param title                titre complet de l'étude
 * @param acronym              acronyme de l'étude
 * @param studyType            type d'étude (enum)
 * @param studyTypeLabel       libellé français du type d'étude
 * @param sponsorType          type de promoteur (enum)
 * @param sponsorTypeLabel     libellé français du type de promoteur
 * @param sponsor              nom du promoteur
 * @param principalInvestigator nom de l'investigateur principal
 * @param therapeuticArea      domaine thérapeutique
 * @param phase                phase de l'essai (enum)
 * @param phaseLabel           libellé de la phase
 * @param currentStatus        statut courant (enum)
 * @param currentStatusLabel   libellé français du statut
 * @param startDate            date de début
 * @param endDate              date de fin
 * @param approvalDate         date d'approbation par les autorités
 * @param targetEnrollment     nombre cible de patients
 * @param currentEnrollment    nombre actuel de patients inclus
 * @param isSponsorCusl        indique si les CUSL sont promoteur
 * @param description          description libre du protocole
 * @param createdAt            timestamp de création
 * @param updatedAt            timestamp de dernière modification
 * @param createdBy            email de l'utilisateur créateur
 */
public record StudyResponse(
    UUID id,
    UUID tenantId,
    String studyNumber,
    String ethicsNumber,
    String eudractNumber,
    String ctisNumber,
    String title,
    String acronym,
    StudyType studyType,
    String studyTypeLabel,
    SponsorType sponsorType,
    String sponsorTypeLabel,
    String sponsor,
    String principalInvestigator,
    String therapeuticArea,
    StudyPhase phase,
    String phaseLabel,
    StudyStatus currentStatus,
    String currentStatusLabel,
    LocalDate startDate,
    LocalDate endDate,
    LocalDate approvalDate,
    Integer targetEnrollment,
    Integer currentEnrollment,
    boolean isSponsorCusl,
    String description,
    Instant createdAt,
    Instant updatedAt,
    String createdBy
) {}
