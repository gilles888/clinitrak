package be.clinitrak.ctc.controller;

import be.clinitrak.ctc.dto.MonitoringVisitCreateRequest;
import be.clinitrak.ctc.dto.MonitoringVisitResponse;
import be.clinitrak.ctc.service.MonitoringVisitService;
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
 * Contrôleur REST pour la gestion des visites de monitoring CTC.
 *
 * <p>Fournit les endpoints de consultation et de création des visites de monitoring.
 * Toutes les opérations sont scoped au tenant courant extrait du {@link TenantContext}.
 */
@Slf4j
@RestController
@RequestMapping("/api/v1/ctc/monitoring-visits")
@RequiredArgsConstructor
@Tag(name = "Visites de Monitoring", description = "Gestion des visites de monitoring des études cliniques")
public class MonitoringVisitController {

    private final MonitoringVisitService service;

    /**
     * Retourne toutes les visites de monitoring du tenant courant.
     *
     * @return 200 OK avec la liste des visites
     */
    @GetMapping
    @PreAuthorize("hasAnyRole('ROLE_CTC_MANAGER', 'ROLE_STUDY_COORDINATOR', 'ROLE_SUPER_ADMIN')")
    @Operation(summary = "Lister les visites", description = "Retourne toutes les visites de monitoring du tenant courant")
    public ResponseEntity<List<MonitoringVisitResponse>> getAll() {
        String tenantId = TenantContext.getTenantId();
        return ResponseEntity.ok(service.getAll(tenantId));
    }

    /**
     * Crée une nouvelle visite de monitoring planifiée.
     *
     * @param request données de création de la visite
     * @return 201 Created avec la visite créée
     */
    @PostMapping
    @PreAuthorize("hasAnyRole('ROLE_CTC_MANAGER', 'ROLE_STUDY_COORDINATOR')")
    @Operation(summary = "Créer une visite", description = "Planifie une nouvelle visite de monitoring")
    public ResponseEntity<MonitoringVisitResponse> create(
        @Valid @RequestBody MonitoringVisitCreateRequest request
    ) {
        String tenantId = TenantContext.getTenantId();
        log.debug("POST /api/v1/ctc/monitoring-visits — création pour étude {}", request.studyId());
        MonitoringVisitResponse response = service.create(request, tenantId);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }
}
