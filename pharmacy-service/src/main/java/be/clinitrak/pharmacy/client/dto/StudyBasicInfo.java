package be.clinitrak.pharmacy.client.dto;

/**
 * DTO minimal représentant les informations de base d'une étude clinique
 * retournées par le study-service via l'appel Feign.
 *
 * @param studyNumber numéro d'étude (ex: ST-2026-00001)
 * @param title       titre complet de l'étude
 * @param status      statut courant de l'étude
 */
public record StudyBasicInfo(
    String studyNumber,
    String title,
    String status
) {}
