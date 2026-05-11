package be.clinitrak.pharmacy.dto;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Réponse contenant les informations d'une levée d'insu d'urgence.
 *
 * <p>Le champ {@code treatment} n'est renseigné qu'après approbation par un pharmacien.
 *
 * @param id           identifiant UUID de la levée d'insu
 * @param studyId      identifiant de l'étude
 * @param patientCode  code anonymisé du patient
 * @param requestDate  date et heure de la demande
 * @param requestedBy  identifiant du demandeur
 * @param reason       raison médicale
 * @param treatment    traitement révélé (null jusqu'à approbation)
 * @param approvedBy   identifiant du pharmacien approbateur
 * @param approvedAt   date et heure de l'approbation
 * @param createdAt    date de création de l'enregistrement
 */
public record EmergencyUnblindingResponse(
    UUID id,
    String studyId,
    String patientCode,
    LocalDateTime requestDate,
    String requestedBy,
    String reason,
    String treatment,
    String approvedBy,
    LocalDateTime approvedAt,
    LocalDateTime createdAt
) {}
