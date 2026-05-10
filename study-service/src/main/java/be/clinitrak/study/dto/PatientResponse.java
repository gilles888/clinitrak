package be.clinitrak.study.dto;

import be.clinitrak.study.domain.enums.PatientStatus;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

/**
 * DTO de réponse pour un patient pseudonymisé.
 *
 * <p>Ne contient aucune donnée directement identifiante conformément au RGPD.
 *
 * @param id            identifiant UUID du patient
 * @param patientCode   code pseudonyme unique dans l'étude
 * @param inclusionDate date d'inclusion dans l'étude
 * @param exclusionDate date de sortie de l'étude
 * @param status        statut actuel du patient
 * @param siteCode      code du centre participant
 * @param notes         notes non identifiantes
 * @param createdAt     timestamp de création dans le système
 */
public record PatientResponse(
    UUID id,
    String patientCode,
    LocalDate inclusionDate,
    LocalDate exclusionDate,
    PatientStatus status,
    String siteCode,
    String notes,
    Instant createdAt
) {}
