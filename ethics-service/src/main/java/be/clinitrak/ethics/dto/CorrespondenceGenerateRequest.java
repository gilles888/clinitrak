package be.clinitrak.ethics.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.util.Map;
import java.util.UUID;

/**
 * Requête de génération d'une correspondance CE depuis un template.
 *
 * @param studyId             identifiant de l'étude concernée (obligatoire)
 * @param reviewId            identifiant de l'avis CE associé (optionnel)
 * @param templateCode        code du template à utiliser (obligatoire)
 * @param recipientEmail      email du destinataire (obligatoire)
 * @param recipientName       nom du destinataire (obligatoire)
 * @param additionalVariables variables supplémentaires pour le template (optionnel)
 */
public record CorrespondenceGenerateRequest(
    @NotNull(message = "L'identifiant de l'étude est obligatoire")
    UUID studyId,

    UUID reviewId,

    @NotBlank(message = "Le code du template est obligatoire")
    String templateCode,

    @NotBlank(message = "L'email du destinataire est obligatoire")
    String recipientEmail,

    @NotBlank(message = "Le nom du destinataire est obligatoire")
    String recipientName,

    Map<String, String> additionalVariables
) {}
