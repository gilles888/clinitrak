package be.clinitrak.study.dto;

import be.clinitrak.study.domain.enums.StudyPhase;
import be.clinitrak.study.domain.enums.StudyStatus;
import be.clinitrak.study.domain.enums.StudyType;
import be.clinitrak.study.domain.enums.SponsorType;

import java.time.LocalDate;
import java.util.UUID;

/**
 * DTO résumé d'une étude clinique, optimisé pour les tableaux de bord et les listes.
 *
 * <p>Retourné par GET /studies (liste paginée).
 * N'inclut pas les champs verbeux comme la description et les timestamps d'audit.
 *
 * @param id                   identifiant UUID de l'étude
 * @param studyNumber          numéro interne de l'étude
 * @param title                titre complet de l'étude
 * @param acronym              acronyme de l'étude
 * @param studyType            type d'étude
 * @param currentStatus        statut courant
 * @param currentStatusLabel   libellé français du statut
 * @param sponsorType          type de promoteur
 * @param sponsor              nom du promoteur
 * @param principalInvestigator nom de l'investigateur principal
 * @param phase                phase de l'essai
 * @param startDate            date de début
 * @param endDate              date de fin
 * @param targetEnrollment     nombre cible de patients
 * @param currentEnrollment    nombre actuel de patients inclus
 * @param isSponsorCusl        indique si les CUSL sont promoteur
 */
public record StudySummaryResponse(
    UUID id,
    String studyNumber,
    String title,
    String acronym,
    StudyType studyType,
    StudyStatus currentStatus,
    String currentStatusLabel,
    SponsorType sponsorType,
    String sponsor,
    String principalInvestigator,
    StudyPhase phase,
    LocalDate startDate,
    LocalDate endDate,
    Integer targetEnrollment,
    Integer currentEnrollment,
    boolean isSponsorCusl
) {}
