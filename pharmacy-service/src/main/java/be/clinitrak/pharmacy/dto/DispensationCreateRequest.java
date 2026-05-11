package be.clinitrak.pharmacy.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDate;
import java.util.UUID;

/**
 * Requête de création d'une dispensation de médicament à un patient.
 *
 * <p>La création d'une dispensation décrémente automatiquement le stock disponible.
 *
 * @param drugId           identifiant du médicament à dispenser (obligatoire)
 * @param studyId          identifiant de l'étude (obligatoire)
 * @param patientCode      code anonymisé du patient (obligatoire)
 * @param dispensationDate date de la dispensation (obligatoire)
 * @param pharmacistId     identifiant du pharmacien (obligatoire)
 * @param prescriberId     identifiant du médecin prescripteur (obligatoire)
 * @param quantity         quantité dispensée (obligatoire)
 * @param prescription     numéro ou référence de l'ordonnance
 * @param visitNumber      numéro de la visite d'étude
 * @param notes            notes libres du pharmacien
 */
public record DispensationCreateRequest(
    @NotNull UUID drugId,
    @NotBlank String studyId,
    @NotBlank String patientCode,
    @NotNull LocalDate dispensationDate,
    @NotBlank String pharmacistId,
    @NotBlank String prescriberId,
    @NotNull Integer quantity,
    String prescription,
    String visitNumber,
    String notes
) {}
