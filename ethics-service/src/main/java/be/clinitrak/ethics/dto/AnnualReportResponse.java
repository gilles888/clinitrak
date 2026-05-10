package be.clinitrak.ethics.dto;

import be.clinitrak.ethics.domain.enums.AnnualReportStatus;

import java.time.LocalDate;
import java.util.UUID;

/**
 * Réponse représentant un rapport annuel attendu par le Comité d'Éthique.
 *
 * @param id              identifiant UUID du rapport
 * @param tenantId        identifiant du tenant
 * @param studyId         identifiant de l'étude
 * @param reportYear      année de référence du rapport
 * @param dueDate         date d'échéance pour la soumission
 * @param receivedDate    date de réception effective (null si non reçu)
 * @param status          statut courant
 * @param statusLabel     libellé français du statut
 * @param reminderSent60  indique si le rappel J-60 a été envoyé
 * @param reminderSent30  indique si le rappel J-30 a été envoyé
 * @param reminderSent0   indique si le rappel J-0 a été envoyé
 * @param lastReminderDate date du dernier rappel envoyé
 * @param notes           notes libres
 * @param daysUntilDue    nombre de jours avant la date d'échéance (négatif si dépassé)
 */
public record AnnualReportResponse(
    UUID id,
    UUID tenantId,
    UUID studyId,
    int reportYear,
    LocalDate dueDate,
    LocalDate receivedDate,
    AnnualReportStatus status,
    String statusLabel,
    boolean reminderSent60,
    boolean reminderSent30,
    boolean reminderSent0,
    LocalDate lastReminderDate,
    String notes,
    long daysUntilDue
) {}
