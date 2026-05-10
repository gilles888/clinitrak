package be.clinitrak.ethics.controller;

import be.clinitrak.ethics.dto.CorrespondenceGenerateRequest;
import be.clinitrak.ethics.dto.CorrespondenceResponse;
import be.clinitrak.ethics.dto.TemplateResponse;
import be.clinitrak.ethics.domain.repository.CorrespondenceTemplateRepository;
import be.clinitrak.ethics.mapper.EthicsMapper;
import be.clinitrak.ethics.service.CorrespondenceService;
import be.clinitrak.ethics.service.PdfGenerationService;
import be.clinitrak.ethics.tenant.TenantContext;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Contrôleur REST pour la gestion des correspondances du Comité d'Éthique.
 *
 * <p>Fournit les endpoints pour générer des correspondances depuis les templates Thymeleaf,
 * envoyer des correspondances, et télécharger les PDFs associés.
 */
@Slf4j
@RestController
@RequestMapping("/api/v1/ethics/correspondence")
@RequiredArgsConstructor
@Tag(name = "Correspondances CE", description = "Génération et gestion des correspondances du Comité d'Éthique")
public class CorrespondenceController {

    private final CorrespondenceService correspondenceService;
    private final PdfGenerationService pdfGenerationService;
    private final CorrespondenceTemplateRepository templateRepository;
    private final EthicsMapper ethicsMapper;

    /**
     * Retourne les templates de correspondance actifs disponibles pour le tenant courant.
     *
     * @return 200 OK avec la liste des templates actifs
     */
    @GetMapping("/templates")
    @PreAuthorize("hasAnyRole('CE_SECRETARY', 'CE_COORDINATOR', 'ADMIN_TENANT', 'SUPER_ADMIN')")
    @Operation(summary = "Templates actifs", description = "Retourne les modèles de correspondance disponibles")
    public ResponseEntity<List<TemplateResponse>> getActiveTemplates() {
        String tenantStr = TenantContext.getTenantId();
        List<TemplateResponse> templates;
        if (tenantStr != null) {
            UUID tenantId = UUID.fromString(tenantStr);
            templates = templateRepository.findActiveTemplatesForTenant(tenantId)
                .stream()
                .map(ethicsMapper::toTemplateResponse)
                .collect(Collectors.toList());
        } else {
            templates = templateRepository.findByIsActiveTrueAndDeletedFalse()
                .stream()
                .map(ethicsMapper::toTemplateResponse)
                .collect(Collectors.toList());
        }
        return ResponseEntity.ok(templates);
    }

    /**
     * Génère une correspondance CE depuis un template Thymeleaf.
     *
     * @param request données de génération (étude, template, destinataire)
     * @return 201 Created avec la correspondance générée
     */
    @PostMapping("/generate")
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasAnyRole('CE_SECRETARY', 'CE_COORDINATOR', 'ADMIN_TENANT', 'SUPER_ADMIN')")
    @Operation(summary = "Générer une correspondance", description = "Génère une correspondance depuis un template Thymeleaf")
    public ResponseEntity<CorrespondenceResponse> generateCorrespondence(
        @Valid @RequestBody CorrespondenceGenerateRequest request
    ) {
        CorrespondenceResponse response = correspondenceService.generateCorrespondence(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    /**
     * Envoie une correspondance au destinataire.
     *
     * @param id identifiant de la correspondance à envoyer
     * @return 200 OK avec la correspondance mise à jour
     */
    @PostMapping("/{id}/send")
    @PreAuthorize("hasAnyRole('CE_SECRETARY', 'CE_COORDINATOR', 'ADMIN_TENANT', 'SUPER_ADMIN')")
    @Operation(summary = "Envoyer une correspondance", description = "Marque la correspondance comme envoyée au destinataire")
    public ResponseEntity<CorrespondenceResponse> sendCorrespondence(@PathVariable UUID id) {
        return ResponseEntity.ok(correspondenceService.sendCorrespondence(id));
    }

    /**
     * Retourne les correspondances d'une étude avec pagination.
     *
     * @param studyId  identifiant de l'étude
     * @param pageable paramètres de pagination
     * @return 200 OK avec la page de correspondances
     */
    @GetMapping("/by-study/{studyId}")
    @PreAuthorize("hasAnyRole('CE_SECRETARY', 'CE_COORDINATOR', 'ADMIN_TENANT', 'SUPER_ADMIN')")
    @Operation(summary = "Correspondances par étude", description = "Retourne les correspondances d'une étude paginées")
    public ResponseEntity<Page<CorrespondenceResponse>> getCorrespondenceByStudy(
        @PathVariable UUID studyId,
        Pageable pageable
    ) {
        return ResponseEntity.ok(correspondenceService.getCorrespondenceByStudy(studyId, pageable));
    }

    /**
     * Télécharge le PDF d'une correspondance.
     *
     * <p>Génère le PDF à la volée depuis le contenu HTML stocké en base.
     *
     * @param id identifiant de la correspondance
     * @return 200 OK avec le PDF en bytes (Content-Type: application/pdf)
     */
    @GetMapping("/{id}/pdf")
    @PreAuthorize("hasAnyRole('CE_SECRETARY', 'CE_COORDINATOR', 'ADMIN_TENANT', 'SUPER_ADMIN')")
    @Operation(summary = "Télécharger le PDF", description = "Génère et télécharge le PDF d'une correspondance")
    public ResponseEntity<byte[]> downloadPdf(@PathVariable UUID id) {
        String htmlContent = correspondenceService.getCorrespondenceContent(id);
        byte[] pdfBytes = pdfGenerationService.generatePdfFromHtml(htmlContent);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_PDF);
        headers.setContentDispositionFormData("attachment", "correspondance-" + id + ".pdf");
        headers.setContentLength(pdfBytes.length);

        return new ResponseEntity<>(pdfBytes, headers, HttpStatus.OK);
    }
}
