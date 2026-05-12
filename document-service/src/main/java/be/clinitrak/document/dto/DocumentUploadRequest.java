package be.clinitrak.document.dto;

import be.clinitrak.document.domain.enums.DocumentType;
import be.clinitrak.document.domain.enums.ModuleSource;
import jakarta.validation.constraints.NotNull;

import java.util.UUID;

/**
 * Requête d'upload d'un document.
 * Les métadonnées sont transmises en multipart/form-data avec le fichier.
 *
 * @param studyId        identifiant de l'étude clinique associée (optionnel)
 * @param moduleSource   module fonctionnel source du document
 * @param documentType   type fonctionnel du document
 * @param description    description libre du document
 */
public record DocumentUploadRequest(
        UUID studyId,

        @NotNull(message = "Le module source est obligatoire")
        ModuleSource moduleSource,

        @NotNull(message = "Le type de document est obligatoire")
        DocumentType documentType,

        String description
) {}
