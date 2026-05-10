package be.clinitrak.ethics.dto;

import be.clinitrak.ethics.domain.enums.MeetingType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDate;

/**
 * Requête de création ou de mise à jour d'une réunion CE.
 *
 * @param meetingDate  date de la réunion (obligatoire)
 * @param meetingTime  heure de début (ex: "14:00", optionnel)
 * @param meetingType  type de réunion (obligatoire)
 * @param location     lieu de la réunion (obligatoire)
 * @param notes        notes libres sur la réunion (optionnel)
 */
public record MeetingCreateRequest(
    @NotNull(message = "La date de réunion est obligatoire")
    LocalDate meetingDate,

    String meetingTime,

    @NotNull(message = "Le type de réunion est obligatoire")
    MeetingType meetingType,

    @NotBlank(message = "Le lieu de la réunion est obligatoire")
    String location,

    String notes
) {}
