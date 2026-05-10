package be.clinitrak.ctc.controller;

import be.clinitrak.ctc.dto.AssignRequest;
import be.clinitrak.ctc.dto.StatusUpdateRequest;
import be.clinitrak.ctc.dto.TrialDeskRequestCreateRequest;
import be.clinitrak.ctc.dto.TrialDeskRequestResponse;
import be.clinitrak.ctc.service.TrialDeskRequestService;
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
import java.util.UUID;

/**
 * Contrôleur REST pour la gestion des demandes desk CTC.
 *
 * <p>Fournit les endpoints CRUD pour les demandes CTC avec contrôle d'accès RBAC.
 * Toutes les opérations sont scoped au tenant courant extrait du {@link TenantContext}.
 */
@Slf4j
@RestController
@RequestMapping("/api/v1/ctc/desk-requests")
@RequiredArgsConstructor
@Tag(name = "Demandes Desk CTC", description = "Gestion des demandes soumises au Centre de Thérapie Cellulaire")
public class TrialDeskRequestController {

    private final TrialDeskRequestService service;

    /**
     * Retourne toutes les demandes desk du tenant courant.
     *
     * @return 200 OK avec la liste des demandes
     */
    @GetMapping
    @PreAuthorize("hasAnyRole('ROLE_CTC_MANAGER', 'ROLE_STUDY_COORDINATOR', 'ROLE_SUPER_ADMIN')")
    @Operation(summary = "Lister les demandes desk", description = "Retourne toutes les demandes du tenant courant")
    public ResponseEntity<List<TrialDeskRequestResponse>> getAll() {
        String tenantId = TenantContext.getTenantId();
        return ResponseEntity.ok(service.getAll(tenantId));
    }

    /**
     * Crée une nouvelle demande desk CTC.
     *
     * @param request données de création de la demande
     * @return 201 Created avec la demande créée
     */
    @PostMapping
    @PreAuthorize("hasAnyRole('ROLE_CTC_MANAGER', 'ROLE_STUDY_COORDINATOR')")
    @Operation(summary = "Créer une demande desk", description = "Soumet une nouvelle demande au desk CTC")
    public ResponseEntity<TrialDeskRequestResponse> create(
        @Valid @RequestBody TrialDeskRequestCreateRequest request
    ) {
        String tenantId = TenantContext.getTenantId();
        log.debug("POST /api/v1/ctc/desk-requests — création pour étude {}", request.studyId());
        TrialDeskRequestResponse response = service.create(request, tenantId);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    /**
     * Assigne une demande desk à un responsable CTC.
     *
     * @param id            identifiant UUID de la demande
     * @param assignRequest données d'assignation
     * @return 200 OK avec la demande mise à jour
     */
    @PatchMapping("/{id}/assign")
    @PreAuthorize("hasRole('ROLE_CTC_MANAGER')")
    @Operation(summary = "Assigner une demande", description = "Assigne la demande à un responsable CTC")
    public ResponseEntity<TrialDeskRequestResponse> assign(
        @PathVariable UUID id,
        @Valid @RequestBody AssignRequest assignRequest
    ) {
        String tenantId = TenantContext.getTenantId();
        log.debug("PATCH /api/v1/ctc/desk-requests/{}/assign", id);
        return ResponseEntity.ok(service.assign(id, assignRequest, tenantId));
    }

    /**
     * Met à jour le statut d'une demande desk.
     *
     * @param id            identifiant UUID de la demande
     * @param statusRequest nouveau statut et notes
     * @return 200 OK avec la demande mise à jour
     */
    @PatchMapping("/{id}/status")
    @PreAuthorize("hasAnyRole('ROLE_CTC_MANAGER', 'ROLE_STUDY_COORDINATOR')")
    @Operation(summary = "Mettre à jour le statut", description = "Change le statut d'une demande desk")
    public ResponseEntity<TrialDeskRequestResponse> updateStatus(
        @PathVariable UUID id,
        @Valid @RequestBody StatusUpdateRequest statusRequest
    ) {
        String tenantId = TenantContext.getTenantId();
        log.debug("PATCH /api/v1/ctc/desk-requests/{}/status → {}", id, statusRequest.status());
        return ResponseEntity.ok(service.updateStatus(id, statusRequest, tenantId));
    }
}
