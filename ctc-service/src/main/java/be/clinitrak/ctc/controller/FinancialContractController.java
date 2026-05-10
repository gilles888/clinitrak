package be.clinitrak.ctc.controller;

import be.clinitrak.ctc.dto.FinancialContractCreateRequest;
import be.clinitrak.ctc.dto.FinancialContractResponse;
import be.clinitrak.ctc.service.FinancialContractService;
import be.clinitrak.ctc.tenant.TenantContext;
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
 * Contrôleur REST pour la gestion des contrats financiers CTC.
 *
 * <p>Fournit les endpoints de consultation et de création des contrats financiers.
 * Toutes les opérations sont scoped au tenant courant extrait du {@link TenantContext}.
 */
@Slf4j
@RestController
@RequestMapping("/api/v1/ctc/financial-contracts")
@RequiredArgsConstructor
@Tag(name = "Contrats Financiers", description = "Gestion des contrats et conventions financières des études")
public class FinancialContractController {

    private final FinancialContractService service;

    /**
     * Retourne tous les contrats financiers du tenant courant.
     *
     * @return 200 OK avec la liste des contrats
     */
    @GetMapping
    @PreAuthorize("hasAnyRole('ROLE_CTC_MANAGER', 'ROLE_SUPER_ADMIN')")
    @Operation(summary = "Lister les contrats", description = "Retourne tous les contrats financiers du tenant courant")
    public ResponseEntity<List<FinancialContractResponse>> getAll() {
        String tenantId = TenantContext.getTenantId();
        return ResponseEntity.ok(service.getAll(tenantId));
    }

    /**
     * Crée un nouveau contrat financier.
     *
     * @param request données de création du contrat
     * @return 201 Created avec le contrat créé
     */
    @PostMapping
    @PreAuthorize("hasRole('ROLE_CTC_MANAGER')")
    @Operation(summary = "Créer un contrat", description = "Enregistre un nouveau contrat financier (statut DRAFT)")
    public ResponseEntity<FinancialContractResponse> create(
        @Valid @RequestBody FinancialContractCreateRequest request
    ) {
        String tenantId = TenantContext.getTenantId();
        log.debug("POST /api/v1/ctc/financial-contracts — création pour étude {}", request.studyId());
        FinancialContractResponse response = service.create(request, tenantId);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }
}
