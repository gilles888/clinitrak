package be.clinitrak.ctc.dto;

import java.util.List;

/**
 * DTO de réponse pour la timeline complète d'une étude clinique.
 *
 * <p>Regroupe tous les événements CTC liés à une étude (visites de monitoring,
 * contrats, événements qualité, demandes desk) triés par date croissante.
 *
 * @param studyId identifiant de l'étude concernée
 * @param events  liste des événements triés chronologiquement
 */
public record StudyTimelineResponse(
    String studyId,
    List<TimelineEvent> events
) {}
