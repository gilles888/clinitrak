package be.clinitrak.ctc.dto;

import java.time.LocalDate;

/**
 * DTO représentant un événement dans la timeline d'une étude clinique.
 *
 * <p>Agrège les événements de nature différente (visites, contrats, qualité, demandes)
 * en une représentation unifiée pour l'affichage chronologique.
 *
 * @param studyId     identifiant de l'étude concernée
 * @param eventType   catégorie d'événement (ex: MONITORING_VISIT, QUALITY_EVENT, etc.)
 * @param title       titre court de l'événement
 * @param description description détaillée de l'événement
 * @param eventDate   date de l'événement pour l'ordonnancement chronologique
 * @param severity    niveau de sévérité (null pour les événements non critiques)
 */
public record TimelineEvent(
    String studyId,
    String eventType,
    String title,
    String description,
    LocalDate eventDate,
    String severity
) {}
