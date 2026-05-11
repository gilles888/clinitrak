package be.clinitrak.pharmacy.dto;

import jakarta.validation.constraints.NotBlank;

/**
 * Requête de levée d'insu d'urgence pour un patient.
 *
 * <p>La levée d'insu révèle le traitement réel d'un patient après validation
 * par un pharmacien responsable. Le traitement n'est disponible qu'après approbation.
 *
 * @param studyId     identifiant de l'étude (obligatoire)
 * @param patientCode code anonymisé du patient (obligatoire)
 * @param requestedBy identifiant du demandeur (médecin ou infirmier) (obligatoire)
 * @param reason      raison médicale justifiant la levée d'insu (obligatoire)
 */
public record EmergencyUnblindingRequest(
    @NotBlank String studyId,
    @NotBlank String patientCode,
    @NotBlank String requestedBy,
    @NotBlank String reason
) {}
