package be.clinitrak.pharmacy.dto;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Réponse contenant les informations complètes d'une dispensation.
 *
 * @param id               identifiant UUID de la dispensation
 * @param drugId           identifiant UUID du médicament dispensé
 * @param drugName         nom du médicament dispensé
 * @param studyId          identifiant de l'étude
 * @param patientCode      code anonymisé du patient
 * @param dispensationDate date de la dispensation
 * @param pharmacistId     identifiant du pharmacien
 * @param prescriberId     identifiant du médecin prescripteur
 * @param quantity         quantité dispensée
 * @param prescription     référence de l'ordonnance
 * @param visitNumber      numéro de la visite d'étude
 * @param returnDate       date de retour du médicament non utilisé
 * @param returnQuantity   quantité retournée
 * @param notes            notes du pharmacien
 * @param createdAt        date de création
 */
public record DispensationResponse(
    UUID id,
    UUID drugId,
    String drugName,
    String studyId,
    String patientCode,
    LocalDate dispensationDate,
    String pharmacistId,
    String prescriberId,
    Integer quantity,
    String prescription,
    String visitNumber,
    LocalDate returnDate,
    Integer returnQuantity,
    String notes,
    LocalDateTime createdAt
) {}
