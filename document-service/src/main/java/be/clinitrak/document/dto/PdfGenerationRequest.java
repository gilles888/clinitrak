package be.clinitrak.document.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.util.Map;

/**
 * Requête de génération d'un PDF à partir d'un template Thymeleaf.
 * Le document généré est automatiquement stocké dans MinIO.
 *
 * @param templateName   nom du template Thymeleaf sans extension (ex: "protocol-summary")
 * @param data           variables à injecter dans le template
 * @param outputFileName nom du fichier PDF de sortie
 * @param studyId        identifiant de l'étude associée au PDF généré (optionnel, sous forme de String UUID)
 * @param moduleSource   module fonctionnel source au format String (ex: "STUDY", "ETHICS")
 */
public record PdfGenerationRequest(
        @NotBlank(message = "Le nom du template est obligatoire")
        String templateName,

        @NotNull(message = "Les données du template sont obligatoires")
        Map<String, Object> data,

        @NotBlank(message = "Le nom du fichier de sortie est obligatoire")
        String outputFileName,

        String studyId,

        String moduleSource
) {}
