package be.clinitrak.pharmacy.controller;

import be.clinitrak.pharmacy.dto.DispensationCreateRequest;
import be.clinitrak.pharmacy.dto.DispensationResponse;
import be.clinitrak.pharmacy.service.DispensationService;
import be.clinitrak.pharmacy.tenant.TenantContext;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Contrôleur REST pour la gestion des dispensations de médicaments.
 *
 * <p>Endpoints disponibles :
 * <ul>
 *   <li>GET /api/v1/pharmacy/dispensations — liste les dispensations du tenant</li>
 *   <li>POST /api/v1/pharmacy/dispensations — crée une dispensation</li>
 *   <li>GET /api/v1/pharmacy/dispensations/patient/{patientCode} — historique patient</li>
 * </ul>
 */
@Slf4j
@RestController
@RequestMapping("/api/v1/pharmacy/dispensations")
@RequiredArgsConstructor
@Tag(name = "Dispensations", description = "Gestion des dispensations de médicaments aux patients")
public class PharmacyDispensationController {

    private final DispensationService dispensationService;

    /**
     * Retourne la liste des dispensations du tenant courant.
     *
     * @return liste des dispensations actives
     */
    @GetMapping
    @PreAuthorize("hasAnyRole('ROLE_PHARMACIST','ROLE_CTC_MANAGER','ROLE_SUPER_ADMIN')")
    @Operation(summary = "Lister les dispensations")
    public ResponseEntity<List<DispensationResponse>> getAll() {
        String tenantId = TenantContext.getTenantId();
        return ResponseEntity.ok(dispensationService.getAll(tenantId));
    }

    /**
     * Crée une nouvelle dispensation et décrémente le stock disponible.
     *
     * @param request données de la dispensation
     * @return dispensation créée avec statut 201
     */
    @PostMapping
    @PreAuthorize("hasRole('ROLE_PHARMACIST')")
    @Operation(summary = "Créer une dispensation", description = "Crée une dispensation et décrémente le stock disponible")
    public ResponseEntity<DispensationResponse> create(@Valid @RequestBody DispensationCreateRequest request) {
        String tenantId = TenantContext.getTenantId();
        DispensationResponse response = dispensationService.create(request, tenantId);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    /**
     * Retourne l'historique de dispensation d'un patient.
     *
     * @param patientCode code anonymisé du patient
     * @return liste des dispensations du patient
     */
    @GetMapping("/patient/{patientCode}")
    @PreAuthorize("hasAnyRole('ROLE_PHARMACIST','ROLE_CTC_MANAGER','ROLE_SUPER_ADMIN')")
    @Operation(summary = "Historique dispensation patient")
    public ResponseEntity<List<DispensationResponse>> getByPatientCode(@PathVariable String patientCode) {
        String tenantId = TenantContext.getTenantId();
        return ResponseEntity.ok(dispensationService.getByPatientCode(patientCode, tenantId));
    }
}
