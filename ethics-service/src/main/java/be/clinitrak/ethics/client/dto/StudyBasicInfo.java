package be.clinitrak.ethics.client.dto;

import java.util.UUID;

/**
 * DTO minimal représentant les informations de base d'une étude clinique
 * retournées par le study-service via l'appel Feign.
 *
 * @param id                   identifiant UUID de l'étude
 * @param studyNumber          numéro d'étude (ex: ST-2026-00001)
 * @param title                titre complet de l'étude
 * @param acronym              acronyme de l'étude
 * @param principalInvestigator nom de l'investigateur principal
 * @param sponsor              nom du promoteur
 * @param currentStatus        statut courant de l'étude
 */
public record StudyBasicInfo(
    UUID id,
    String studyNumber,
    String title,
    String acronym,
    String principalInvestigator,
    String sponsor,
    String currentStatus
) {}
