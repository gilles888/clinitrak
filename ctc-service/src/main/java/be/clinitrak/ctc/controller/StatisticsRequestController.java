package be.clinitrak.ctc.controller;

import be.clinitrak.ctc.dto.StatisticsRequestCreateRequest;
import be.clinitrak.ctc.dto.StatisticsRequestResponse;
import be.clinitrak.ctc.service.StatisticsRequestService;
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
 * Contrôleur REST pour la gestion des demandes d'analyses statistiques CTC.
 *
 * <p>Fournit les endpoints de consultation et de création des demandes statistiques.
 * Toutes les opérations sont scoped au tenant courant extrait du {@link TenantContext}.
 */
@Slf4j
@RestController
@RequestMapping("/api/v1/ctc/statistics-requests")
@RequiredArgsConstructor
@Tag(name = "Demandes Statistiques", description = "Gestion des demandes d'analyses statistiques au biostatisticien")
public class StatisticsRequestController {

    private final StatisticsRequestService service;

    /**
     * Retourne toutes les demandes statistiques du tenant courant.
     *
     * @return 200 OK avec la liste des demandes
     */
    @GetMapping
    @PreAuthorize("hasAnyRole('ROLE_CTC_MANAGER', 'ROLE_STUDY_COORDINATOR', 'ROLE_BIOSTATISTICIAN', 'ROLE_SUPER_ADMIN')")
    @Operation(summary = "Lister les demandes", description = "Retourne toutes les demandes statistiques du tenant courant")
    public ResponseEntity<List<StatisticsRequestResponse>> getAll() {
        String tenantId = TenantContext.getTenantId();
        return ResponseEntity.ok(service.getAll(tenantId));
    }

    /**
     * Crée une nouvelle demande d'analyse statistique.
     *
     * @param request données de création de la demande
     * @return 201 Created avec la demande créée
     */
    @PostMapping
    @PreAuthorize("hasAnyRole('ROLE_STUDY_COORDINATOR', 'ROLE_CTC_MANAGER')")
    @Operation(summary = "Créer une demande", description = "Soumet une demande d'analyse statistique au biostatisticien")
    public ResponseEntity<StatisticsRequestResponse> create(
        @Valid @RequestBody StatisticsRequestCreateRequest request
    ) {
        String tenantId = TenantContext.getTenantId();
        log.debug("POST /api/v1/ctc/statistics-requests — création pour étude {}", request.studyId());
        StatisticsRequestResponse response = service.create(request, tenantId);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }
}
