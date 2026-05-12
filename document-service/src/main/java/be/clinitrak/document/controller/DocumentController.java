package be.clinitrak.document.controller;

import be.clinitrak.document.domain.enums.DocumentType;
import be.clinitrak.document.dto.DocumentResponse;
import be.clinitrak.document.dto.DocumentUploadRequest;
import be.clinitrak.document.dto.PdfGenerationRequest;
import be.clinitrak.document.service.DocumentService;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.UUID;

/**
 * Contrôleur REST du document-service.
 * Expose les endpoints d'upload, téléchargement, listage, suppression,
 * génération PDF et historique de versions.
 */
@Slf4j
@RestController
@RequestMapping("/api/v1/documents")
@RequiredArgsConstructor
@Tag(name = "Documents", description = "Gestion documentaire (GED) — stockage MinIO, versioning, génération PDF")
public class DocumentController {

    private final DocumentService documentService;
    private final ObjectMapper objectMapper;

    /**
     * Uploade un nouveau document.
     * Accepte du multipart/form-data avec le fichier et les métadonnées JSON.
     *
     * @param file     fichier à uploader
     * @param metadata métadonnées JSON sérialisées dans un part "metadata"
     * @param auth     authentication Spring Security (email extrait du JWT)
     * @return 201 avec les métadonnées du document créé et l'URL de téléchargement
     */
    @PostMapping(value = "/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Operation(summary = "Uploader un document", description = "Upload multipart avec fichier + métadonnées JSON")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<DocumentResponse> upload(
            @RequestPart("file") MultipartFile file,
            @RequestPart("metadata") String metadata,
            Authentication auth) throws Exception {

        DocumentUploadRequest request = objectMapper.readValue(metadata, DocumentUploadRequest.class);
        String uploadedBy = auth.getName();
        DocumentResponse response = documentService.upload(file, request, uploadedBy);
        log.info("Document uploadé par {} : id={}", uploadedBy, response.id());
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    /**
     * Génère une URL de téléchargement présignée pour un document.
     *
     * @param id identifiant du document
     * @return 200 avec les métadonnées et l'URL présignée valable 15 minutes
     */
    @GetMapping({"/{id}/download", "/{id}/download-url"})
    @Operation(summary = "Obtenir l'URL de téléchargement",
               description = "Retourne une URL présignée MinIO valable 15 minutes")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<DocumentResponse> download(
            @Parameter(description = "Identifiant UUID du document") @PathVariable UUID id) {
        return ResponseEntity.ok(documentService.download(id));
    }

    /**
     * Recherche paginée de documents selon des critères optionnels.
     *
     * @param studyId filtrage par étude (optionnel)
     * @param type    filtrage par type de document (optionnel)
     * @param page    numéro de page (0-based)
     * @param size    taille de la page
     * @return 200 avec page de documents
     */
    @GetMapping
    @Operation(summary = "Lister les documents", description = "Recherche paginée avec filtres optionnels")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<Page<DocumentResponse>> findAll(
            @Parameter(description = "Filtrer par étude") @RequestParam(required = false) UUID studyId,
            @Parameter(description = "Filtrer par type") @RequestParam(required = false) DocumentType type,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {

        Pageable pageable = PageRequest.of(page, size);
        return ResponseEntity.ok(documentService.findAll(studyId, type, pageable));
    }

    /**
     * Supprime logiquement un document et son objet MinIO.
     *
     * @param id identifiant du document à supprimer
     * @return 204 No Content
     */
    @DeleteMapping("/{id}")
    @Operation(summary = "Supprimer un document")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN_TENANT', 'CE_COORDINATOR', 'CTC_PM')")
    public ResponseEntity<Void> delete(@PathVariable UUID id) {
        documentService.delete(id);
        return ResponseEntity.noContent().build();
    }

    /**
     * Génère un PDF depuis un template Thymeleaf et le stocke dans MinIO.
     *
     * @param request paramètres de génération (template, données, nom fichier)
     * @param auth    authentication Spring Security
     * @return 201 avec les métadonnées du PDF généré
     */
    @PostMapping("/generate-pdf")
    @Operation(summary = "Générer un PDF", description = "Génère un PDF depuis un template Thymeleaf")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<DocumentResponse> generatePdf(
            @Valid @RequestBody PdfGenerationRequest request,
            Authentication auth) {
        String generatedBy = auth.getName();
        DocumentResponse response = documentService.generatePdf(request, generatedBy);
        log.info("PDF généré par {} : template={}", generatedBy, request.templateName());
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    /**
     * Récupère l'historique de toutes les versions d'un document.
     *
     * @param id identifiant de n'importe quelle version du document
     * @return 200 avec liste triée par version croissante
     */
    @GetMapping("/{id}/versions")
    @Operation(summary = "Historique des versions", description = "Retourne toutes les versions d'un document")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<List<DocumentResponse>> getVersionHistory(@PathVariable UUID id) {
        return ResponseEntity.ok(documentService.getVersionHistory(id));
    }
}
