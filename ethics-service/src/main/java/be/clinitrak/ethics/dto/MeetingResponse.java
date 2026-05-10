package be.clinitrak.ethics.dto;

import be.clinitrak.ethics.domain.enums.MeetingStatus;
import be.clinitrak.ethics.domain.enums.MeetingType;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

/**
 * Réponse représentant une réunion du Comité d'Éthique.
 *
 * @param id               identifiant UUID de la réunion
 * @param tenantId         identifiant du tenant
 * @param meetingDate      date de la réunion
 * @param meetingTime      heure de début
 * @param meetingType      type de réunion
 * @param meetingTypeLabel libellé français du type
 * @param location         lieu de la réunion
 * @param status           statut courant
 * @param statusLabel      libellé français du statut
 * @param notes            notes libres
 * @param agendaItemCount  nombre de points à l'ordre du jour
 * @param createdAt        timestamp de création
 */
public record MeetingResponse(
    UUID id,
    UUID tenantId,
    LocalDate meetingDate,
    String meetingTime,
    MeetingType meetingType,
    String meetingTypeLabel,
    String location,
    MeetingStatus status,
    String statusLabel,
    String notes,
    int agendaItemCount,
    Instant createdAt
) {}
