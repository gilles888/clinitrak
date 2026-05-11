package be.clinitrak.pharmacy.controller;

import be.clinitrak.pharmacy.dto.EmergencyUnblindingRequest;
import be.clinitrak.pharmacy.dto.EmergencyUnblindingResponse;
import be.clinitrak.pharmacy.dto.PharmacyAlertsResponse;
import be.clinitrak.pharmacy.dto.PharmacyDashboardResponse;
import be.clinitrak.pharmacy.service.EmergencyUnblindingService;
import be.clinitrak.pharmacy.service.PharmacyAlertService;
import be.clinitrak.pharmacy.service.PharmacyDashboardService;
import be.clinitrak.pharmacy.service.PdfReportService;
import be.clinitrak.pharmacy.tenant.TenantContext;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

/**
 * Contrôleur REST pour le tableau de bord, les alertes, les rapports
 * et les levées d'insu d'urgence.
 *
 * <p>Endpoints disponibles :
 * <ul>
 *   <li>GET /api/v1/pharmacy/dashboard — tableau de bord</li>
 *   <li>GET /api/v1/pharmacy/alerts — alertes actives</li>
 *   <li>GET /api/v1/pharmacy/reports/inventory — rapport PDF d'inventaire</li>
 *   <li>POST /api/v1/pharmacy/emergency-unblinding — demande de levée d'insu</li>
 *   <li>PATCH /api/v1/pharmacy/emergency-unblinding/{id}/approve — approbation</li>
 * </ul>
 */
@Slf4j
@RestController
@RequestMapping("/api/v1/pharmacy")
@RequiredArgsConstructor
@Tag(name = "Dashboard & Alertes", description = "Tableau de bord, alertes, rapports et levée d'insu")
public class PharmacyDashboardController {

    private final PharmacyDashboardService dashboardService;
    private final PharmacyAlertService alertService;
    private final PdfReportService pdfReportService;
    private final EmergencyUnblindingService unblindingService;

    /**
     * Retourne les indicateurs clés du tableau de bord pharmacie.
     *
     * @return tableau de bord avec tous les indicateurs
     */
    @GetMapping("/dashboard")
    @PreAuthorize("hasAnyRole('ROLE_PHARMACIST','ROLE_CTC_MANAGER','ROLE_SUPER_ADMIN')")
    @Operation(summary = "Tableau de bord pharmacie")
    public ResponseEntity<PharmacyDashboardResponse> getDashboard() {
        String tenantId = TenantContext.getTenantId();
        return ResponseEntity.ok(dashboardService.getDashboard(tenantId));
    }

    /**
     * Retourne les alertes actives du tenant courant.
     *
     * @return liste des alertes avec le nombre d'alertes critiques
     */
    @GetMapping("/alerts")
    @PreAuthorize("hasAnyRole('ROLE_PHARMACIST','ROLE_CTC_MANAGER','ROLE_SUPER_ADMIN')")
    @Operation(summary = "Alertes pharmacie", description = "Retourne les alertes de stock faible et de péremption imminente")
    public ResponseEntity<PharmacyAlertsResponse> getAlerts() {
        String tenantId = TenantContext.getTenantId();
        return ResponseEntity.ok(alertService.getAlerts(tenantId));
    }

    /**
     * Génère et retourne un rapport d'inventaire au format PDF.
     *
     * @return fichier PDF du rapport d'inventaire
     */
    @GetMapping(value = "/reports/inventory", produces = MediaType.APPLICATION_PDF_VALUE)
    @PreAuthorize("hasAnyRole('ROLE_PHARMACIST','ROLE_SUPER_ADMIN')")
    @Operation(summary = "Rapport d'inventaire PDF")
    public ResponseEntity<byte[]> getInventoryReport() {
        String tenantId = TenantContext.getTenantId();
        byte[] pdf = pdfReportService.generateInventoryReport(tenantId);
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_PDF);
        headers.setContentDispositionFormData("attachment", "rapport-inventaire-pharmacie.pdf");
        return ResponseEntity.ok().headers(headers).body(pdf);
    }

    /**
     * Crée une demande de levée d'insu d'urgence.
     *
     * <p>Le traitement n'est pas révélé — une approbation est nécessaire.
     *
     * @param request données de la demande
     * @return levée d'insu créée avec statut 201
     */
    @PostMapping("/emergency-unblinding")
    @PreAuthorize("hasAnyRole('ROLE_PHARMACIST','ROLE_SUPER_ADMIN')")
    @Operation(summary = "Demande de levée d'insu d'urgence")
    public ResponseEntity<EmergencyUnblindingResponse> requestUnblinding(
        @Valid @RequestBody EmergencyUnblindingRequest request
    ) {
        String tenantId = TenantContext.getTenantId();
        EmergencyUnblindingResponse response = unblindingService.request(request, tenantId);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    /**
     * Approuve une levée d'insu et révèle le traitement réel.
     *
     * @param id         identifiant UUID de la levée d'insu
     * @param approvedBy identifiant du pharmacien approbateur
     * @return levée d'insu approuvée avec le traitement révélé
     */
    @PatchMapping("/emergency-unblinding/{id}/approve")
    @PreAuthorize("hasAnyRole('ROLE_PHARMACIST','ROLE_SUPER_ADMIN')")
    @Operation(summary = "Approuver une levée d'insu", description = "Révèle le code de randomisation du patient")
    public ResponseEntity<EmergencyUnblindingResponse> approveUnblinding(
        @PathVariable UUID id,
        @RequestParam String approvedBy
    ) {
        String tenantId = TenantContext.getTenantId();
        EmergencyUnblindingResponse response = unblindingService.approve(id, approvedBy, tenantId);
        return ResponseEntity.ok(response);
    }
}
