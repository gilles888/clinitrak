package be.clinitrak.document.dto;

import be.clinitrak.document.domain.enums.DocumentType;
import be.clinitrak.document.domain.enums.ModuleSource;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Réponse contenant les métadonnées d'un document et l'URL de téléchargement signée.
 *
 * @param id              identifiant unique du document
 * @param fileName        nom du fichier stocké dans MinIO
 * @param originalFileName nom original du fichier fourni par l'utilisateur
 * @param documentType    type fonctionnel du document
 * @param moduleSource    module fonctionnel source
 * @param studyId         identifiant de l'étude associée
 * @param version         numéro de version du document
 * @param uploadedBy      email de l'utilisateur ayant uploadé le document
 * @param uploadedAt      date et heure de l'upload
 * @param downloadUrl     URL présignée MinIO valable 15 minutes (null si non demandé)
 * @param fileSize        taille du fichier en octets
 * @param description     description du document
 */
public record DocumentResponse(
        UUID id,
        String fileName,
        String originalFileName,
        DocumentType documentType,
        ModuleSource moduleSource,
        UUID studyId,
        int version,
        String uploadedBy,
        LocalDateTime uploadedAt,
        String downloadUrl,
        Long fileSize,
        String description
) {}
