package be.clinitrak.document.service;

import be.clinitrak.document.domain.entity.Document;
import be.clinitrak.document.domain.enums.DocumentType;
import be.clinitrak.document.domain.enums.ModuleSource;
import be.clinitrak.document.domain.repository.DocumentRepository;
import be.clinitrak.document.dto.DocumentResponse;
import be.clinitrak.document.dto.DocumentUploadRequest;
import be.clinitrak.document.dto.PdfGenerationRequest;
import be.clinitrak.document.exception.DocumentNotFoundException;
import be.clinitrak.document.exception.StorageException;
import be.clinitrak.document.mapper.DocumentMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import org.thymeleaf.context.Context;
import org.thymeleaf.spring6.SpringTemplateEngine;
import org.xhtmlrenderer.pdf.ITextRenderer;

import java.io.ByteArrayOutputStream;
import java.util.List;
import java.util.UUID;

/**
 * Service métier du document-service.
 * Gère le cycle de vie complet des documents : upload avec versioning,
 * téléchargement, suppression logique et génération de PDF.
 */
@Slf4j
@Service
@Transactional
@RequiredArgsConstructor
public class DocumentService {

    private final DocumentRepository documentRepository;
    private final DocumentStorageService storageService;
    private final DocumentMapper documentMapper;
    private final SpringTemplateEngine templateEngine;

    /** Nom du bucket MinIO par défaut. */
    private static final String DEFAULT_BUCKET = "clinitrak-documents";

    /**
     * Uploade un document dans MinIO et enregistre ses métadonnées en base.
     * Si un document avec les mêmes critères (studyId + type + originalFileName) existe déjà,
     * crée automatiquement une nouvelle version.
     *
     * @param file       fichier à uploader
     * @param request    métadonnées du document
     * @param uploadedBy email de l'utilisateur connecté (extrait du JWT)
     * @return réponse contenant les métadonnées et l'URL de téléchargement
     */
    public DocumentResponse upload(MultipartFile file, DocumentUploadRequest request, String uploadedBy) {
        String originalFileName = file.getOriginalFilename() != null
                ? file.getOriginalFilename() : "document";
        String uuid = UUID.randomUUID().toString();
        String extension = extractExtension(originalFileName);
        String storedFileName = uuid + extension;
        String objectKey = buildObjectKey(request.studyId(), request.documentType(), storedFileName);

        // Calcul du numéro de version
        int version = 1;
        UUID parentId = null;

        // Pour le versioning, on cherche un document existant non supprimé avec le même originalFileName
        // et les mêmes critères de classification
        List<Document> existingVersions = documentRepository.findVersionHistory(
                findRootDocumentId(request.studyId(), request.moduleSource(), request.documentType(), originalFileName));
        if (!existingVersions.isEmpty()) {
            Document latestVersion = existingVersions.get(existingVersions.size() - 1);
            version = latestVersion.getVersion() + 1;
            parentId = latestVersion.getId();
        }

        // Upload dans MinIO
        storageService.uploadFile(file, objectKey);

        // Persistance en base
        Document document = Document.builder()
                .fileName(storedFileName)
                .originalFileName(originalFileName)
                .contentType(file.getContentType())
                .fileSize(file.getSize())
                .bucketName(DEFAULT_BUCKET)
                .objectKey(objectKey)
                .documentType(request.documentType())
                .moduleSource(request.moduleSource())
                .studyId(request.studyId())
                .version(version)
                .parentDocumentId(parentId)
                .uploadedBy(uploadedBy)
                .description(request.description())
                .build();

        Document saved = documentRepository.save(document);
        log.info("Document uploadé : id={}, version={}, uploadedBy={}", saved.getId(), version, uploadedBy);

        String downloadUrl = storageService.generatePresignedDownloadUrl(objectKey);
        DocumentResponse response = documentMapper.toResponse(saved);
        return withDownloadUrl(response, downloadUrl);
    }

    /**
     * Génère l'URL de téléchargement présignée pour un document.
     *
     * @param id identifiant du document
     * @return réponse contenant les métadonnées et l'URL présignée
     * @throws DocumentNotFoundException si le document n'existe pas ou est supprimé
     */
    @Transactional(readOnly = true)
    public DocumentResponse download(UUID id) {
        Document document = documentRepository.findActiveById(id)
                .orElseThrow(() -> new DocumentNotFoundException(id));
        String downloadUrl = storageService.generatePresignedDownloadUrl(document.getObjectKey());
        DocumentResponse response = documentMapper.toResponse(document);
        return withDownloadUrl(response, downloadUrl);
    }

    /**
     * Recherche paginée de documents selon des critères optionnels.
     *
     * @param studyId  filtrage par étude (optionnel)
     * @param type     filtrage par type de document (optionnel)
     * @param pageable paramètres de pagination
     * @return page de DocumentResponse (sans URL de téléchargement)
     */
    @Transactional(readOnly = true)
    public Page<DocumentResponse> findAll(UUID studyId, DocumentType type, Pageable pageable) {
        return documentRepository.findAllByCriteria(studyId, type, pageable)
                .map(documentMapper::toResponse);
    }

    /**
     * Supprime logiquement un document de la base et physiquement de MinIO.
     *
     * @param id identifiant du document à supprimer
     * @throws DocumentNotFoundException si le document n'existe pas ou est déjà supprimé
     */
    public void delete(UUID id) {
        Document document = documentRepository.findActiveById(id)
                .orElseThrow(() -> new DocumentNotFoundException(id));

        // Soft delete en base
        document.softDelete();
        documentRepository.save(document);

        // Suppression physique dans MinIO
        try {
            storageService.deleteObject(document.getObjectKey());
        } catch (StorageException ex) {
            log.warn("Erreur lors de la suppression MinIO pour le document {} : {}", id, ex.getMessage());
        }

        log.info("Document supprimé (soft-delete) : id={}", id);
    }

    /**
     * Génère un PDF à partir d'un template Thymeleaf et l'uploade dans MinIO.
     *
     * @param request paramètres de génération (template, données, nom fichier)
     * @param generatedBy email de l'utilisateur demandant la génération
     * @return réponse contenant les métadonnées du PDF généré et l'URL de téléchargement
     */
    public DocumentResponse generatePdf(PdfGenerationRequest request, String generatedBy) {
        // Rendu HTML via Thymeleaf
        Context context = new Context();
        if (request.data() != null) {
            request.data().forEach(context::setVariable);
        }
        String htmlContent = templateEngine.process("pdf/" + request.templateName(), context);

        // Conversion HTML → PDF via Flying Saucer
        byte[] pdfContent;
        try (ByteArrayOutputStream outputStream = new ByteArrayOutputStream()) {
            ITextRenderer renderer = new ITextRenderer();
            renderer.setDocumentFromString(htmlContent);
            renderer.layout();
            renderer.createPDF(outputStream);
            pdfContent = outputStream.toByteArray();
        } catch (Exception ex) {
            throw new StorageException("Erreur lors de la génération du PDF : " + ex.getMessage(), ex);
        }

        // Stockage dans MinIO
        String uuid = UUID.randomUUID().toString();
        String fileName = uuid + "-" + request.outputFileName();
        UUID studyIdUuid = request.studyId() != null ? UUID.fromString(request.studyId()) : null;
        ModuleSource moduleSource = request.moduleSource() != null
                ? ModuleSource.valueOf(request.moduleSource()) : ModuleSource.STUDY;
        String objectKey = buildObjectKey(studyIdUuid, DocumentType.PDF, fileName);
        storageService.uploadBytes(pdfContent, objectKey, "application/pdf");

        // Persistance des métadonnées
        Document document = Document.builder()
                .fileName(fileName)
                .originalFileName(request.outputFileName())
                .contentType("application/pdf")
                .fileSize((long) pdfContent.length)
                .bucketName(DEFAULT_BUCKET)
                .objectKey(objectKey)
                .documentType(DocumentType.PDF)
                .moduleSource(moduleSource)
                .studyId(studyIdUuid)
                .version(1)
                .uploadedBy(generatedBy)
                .description("PDF généré depuis le template : " + request.templateName())
                .build();

        Document saved = documentRepository.save(document);
        log.info("PDF généré et uploadé : id={}, template={}, size={} bytes",
                saved.getId(), request.templateName(), pdfContent.length);

        String downloadUrl = storageService.generatePresignedDownloadUrl(objectKey);
        DocumentResponse response = documentMapper.toResponse(saved);
        return withDownloadUrl(response, downloadUrl);
    }

    /**
     * Récupère l'historique complet des versions d'un document.
     *
     * @param id identifiant de n'importe quelle version du document
     * @return liste de toutes les versions triées par numéro croissant
     * @throws DocumentNotFoundException si le document n'existe pas
     */
    @Transactional(readOnly = true)
    public List<DocumentResponse> getVersionHistory(UUID id) {
        documentRepository.findActiveById(id)
                .orElseThrow(() -> new DocumentNotFoundException(id));
        List<Document> versions = documentRepository.findVersionHistory(id);
        return documentMapper.toResponseList(versions);
    }

    // -------------------------------------------------------------------------
    // Méthodes privées utilitaires
    // -------------------------------------------------------------------------

    /**
     * Construit la clé objet MinIO au format : studyId/documentType/fileName.
     */
    private String buildObjectKey(UUID studyId, DocumentType type, String fileName) {
        String prefix = studyId != null ? studyId.toString() : "global";
        return prefix + "/" + type.name() + "/" + fileName;
    }

    /**
     * Extrait l'extension du fichier original.
     */
    private String extractExtension(String fileName) {
        int dotIndex = fileName.lastIndexOf('.');
        return dotIndex >= 0 ? fileName.substring(dotIndex) : "";
    }

    /**
     * Trouve l'identifiant racine du document pour le versioning.
     * Retourne un UUID aléatoire si aucun document correspondant n'est trouvé
     * (ce qui entraîne la création d'une version 1 fraîche).
     */
    private UUID findRootDocumentId(UUID studyId, ModuleSource moduleSource,
                                     DocumentType documentType, String originalFileName) {
        // Recherche un document existant non supprimé avec les mêmes critères
        return documentRepository.findAllByCriteria(studyId, documentType, Pageable.unpaged())
                .stream()
                .filter(doc -> !doc.isDeleted()
                        && doc.getModuleSource() == moduleSource
                        && originalFileName.equals(doc.getOriginalFileName()))
                .findFirst()
                .map(doc -> doc.getParentDocumentId() != null ? doc.getParentDocumentId() : doc.getId())
                .orElse(UUID.randomUUID()); // Aucun existant → version 1
    }

    /**
     * Crée une nouvelle instance DocumentResponse avec l'URL de téléchargement renseignée.
     */
    private DocumentResponse withDownloadUrl(DocumentResponse original, String downloadUrl) {
        return new DocumentResponse(
                original.id(),
                original.fileName(),
                original.originalFileName(),
                original.documentType(),
                original.moduleSource(),
                original.studyId(),
                original.version(),
                original.uploadedBy(),
                original.uploadedAt(),
                downloadUrl,
                original.fileSize(),
                original.description());
    }
}
