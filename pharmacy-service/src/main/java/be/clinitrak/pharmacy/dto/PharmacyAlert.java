package be.clinitrak.pharmacy.dto;

import be.clinitrak.pharmacy.domain.enums.AlertType;

import java.time.LocalDate;

/**
 * Alerte pharmacie générée automatiquement par le service d'alerte.
 *
 * @param type      type d'alerte (LOW_STOCK, EXPIRY_7, EXPIRY_30, QUARANTINE)
 * @param typeLabel libellé français du type d'alerte
 * @param studyId   identifiant de l'étude concernée
 * @param drugName  nom du médicament concerné
 * @param detail    détail de l'alerte (ex: "Quantité: 5 comprimés", "Expire le 2026-05-20")
 * @param alertDate date associée à l'alerte (date d'expiration ou date du jour)
 */
public record PharmacyAlert(
    AlertType type,
    String typeLabel,
    String studyId,
    String drugName,
    String detail,
    LocalDate alertDate
) {}
