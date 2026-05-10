package be.clinitrak.ctc.controller;

import be.clinitrak.ctc.dto.*;
import be.clinitrak.ctc.service.*;
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
 * Contrôleur REST pour le tableau de bord CTC, les études sponsor et les événements qualité.
 *
 * <p>Regroupe les endpoints de consultation agrégée du CTC, la gestion des études
 * sponsor CUSL, des événements qualité et de la timeline par étude.
 * Toutes les opérations sont scoped au tenant courant extrait du {@link TenantContext}.
 */
@Slf4j
@RestController
@RequestMapping("/api/v1/ctc")
@RequiredArgsConstructor
@Tag(name = "Tableau de Bord CTC", description = "Vue d'ensemble du Centre de Thérapie Cellulaire")
public class CTCDashboardController {

    private final CTCDashboardService dashboardService;
    private final SponsorStudyService sponsorStudyService;
    private final QualityEventService qualityEventService;
    private final StudyTimelineService studyTimelineService;

    /**
     * Retourne le tableau de bord CTC du tenant courant.
     *
     * <p>Agrège : demandes en attente, visites planifiées, événements qualité ouverts,
     * contrats actifs, statistiques pendantes et 5 dernières demandes.
     *
     * @return 200 OK avec les indicateurs du tableau de bord
     */
    @GetMapping("/dashboard")
    @PreAuthorize("hasAnyRole('ROLE_CTC_MANAGER', 'ROLE_SUPER_ADMIN')")
    @Operation(summary = "Tableau de bord", description = "Retourne les indicateurs clés du CTC")
    public ResponseEntity<CTCDashboardResponse> getDashboard() {
        return ResponseEntity.ok(dashboardService.getDashboard());
    }

    /**
     * Retourne toutes les études sponsor CUSL du tenant courant.
     *
     * @return 200 OK avec la liste des études sponsor
     */
    @GetMapping("/sponsor-studies")
    @PreAuthorize("hasAnyRole('ROLE_CTC_MANAGER', 'ROLE_SUPER_ADMIN')")
    @Operation(summary = "Lister les études sponsor", description = "Retourne les études dont le CUSL est promoteur")
    public ResponseEntity<List<SponsorStudyResponse>> getSponsorStudies() {
        String tenantId = TenantContext.getTenantId();
        return ResponseEntity.ok(sponsorStudyService.getAll(tenantId));
    }

    /**
     * Crée une nouvelle étude sponsor CUSL.
     *
     * @param request données de création de l'étude sponsor
     * @return 201 Created avec l'étude créée
     */
    @PostMapping("/sponsor-studies")
    @PreAuthorize("hasRole('ROLE_CTC_MANAGER')")
    @Operation(summary = "Créer une étude sponsor", description = "Enregistre une étude dont le CUSL est promoteur")
    public ResponseEntity<SponsorStudyResponse> createSponsorStudy(
        @Valid @RequestBody SponsorStudyCreateRequest request
    ) {
        String tenantId = TenantContext.getTenantId();
        log.debug("POST /api/v1/ctc/sponsor-studies — studyId {}", request.studyId());
        SponsorStudyResponse response = sponsorStudyService.create(request, tenantId);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    /**
     * Retourne tous les événements qualité du tenant courant.
     *
     * @return 200 OK avec la liste des événements qualité
     */
    @GetMapping("/quality-events")
    @PreAuthorize("hasAnyRole('ROLE_CTC_MANAGER', 'ROLE_SUPER_ADMIN', 'ROLE_QUALITY_MANAGER')")
    @Operation(summary = "Lister les événements qualité", description = "Retourne tous les événements qualité du tenant")
    public ResponseEntity<List<QualityEventResponse>> getQualityEvents() {
        String tenantId = TenantContext.getTenantId();
        return ResponseEntity.ok(qualityEventService.getAll(tenantId));
    }

    /**
     * Crée un nouvel événement qualité.
     *
     * @param request données de création de l'événement
     * @return 201 Created avec l'événement créé
     */
    @PostMapping("/quality-events")
    @PreAuthorize("hasAnyRole('ROLE_CTC_MANAGER', 'ROLE_QUALITY_MANAGER')")
    @Operation(summary = "Créer un événement qualité", description = "Enregistre un nouveau événement qualité")
    public ResponseEntity<QualityEventResponse> createQualityEvent(
        @Valid @RequestBody QualityEventCreateRequest request
    ) {
        String tenantId = TenantContext.getTenantId();
        log.debug("POST /api/v1/ctc/quality-events — type {}", request.eventType());
        QualityEventResponse response = qualityEventService.create(request, tenantId);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    /**
     * Retourne la timeline complète d'une étude pour le tenant courant.
     *
     * <p>Agrège et trie chronologiquement tous les événements CTC de l'étude :
     * demandes desk, visites de monitoring, contrats financiers et événements qualité.
     *
     * @param studyId identifiant de l'étude dans le study-service
     * @return 200 OK avec la timeline de l'étude
     */
    @GetMapping("/timeline/{studyId}")
    @PreAuthorize("hasAnyRole('ROLE_CTC_MANAGER', 'ROLE_STUDY_COORDINATOR', 'ROLE_SUPER_ADMIN')")
    @Operation(summary = "Timeline d'une étude", description = "Retourne la timeline chronologique d'une étude")
    public ResponseEntity<StudyTimelineResponse> getTimeline(@PathVariable String studyId) {
        String tenantId = TenantContext.getTenantId();
        log.debug("GET /api/v1/ctc/timeline/{}", studyId);
        return ResponseEntity.ok(studyTimelineService.getTimeline(studyId, tenantId));
    }
}
