package be.clinitrak.ctc.client.dto;

/**
 * Informations de base d'une étude clinique retournées par le study-service.
 *
 * @param studyNumber numéro d'identification de l'étude
 * @param title       titre complet de l'étude
 * @param status      statut courant de l'étude dans le study-service
 */
public record StudyBasicInfo(
    String studyNumber,
    String title,
    String status
) {}
