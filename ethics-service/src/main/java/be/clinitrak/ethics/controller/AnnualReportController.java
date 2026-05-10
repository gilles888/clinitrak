package be.clinitrak.ethics.controller;

import be.clinitrak.ethics.dto.AnnualReportResponse;
import be.clinitrak.ethics.service.AnnualReportService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

/**
 * Contrôleur REST pour la gestion des rapports annuels du Comité d'Éthique.
 *
 * <p>Fournit les endpoints pour créer, consulter et gérer les rapports annuels
 * attendus par le CE, ainsi qu'un endpoint batch pour déclencher les vérifications
 * de rappels manuellement.
 */
@Slf4j
@RestController
@RequestMapping("/api/v1/ethics/annual-reports")
@RequiredArgsConstructor
@Tag(name = "Rapports Annuels", description = "Gestion des rapports annuels du Comité d'Éthique")
public class AnnualReportController {

    private final AnnualReportService annualReportService;

    /**
     * Retourne les rapports annuels du tenant courant avec pagination.
     *
     * @param pageable paramètres de pagination
     * @return 200 OK avec la page de rapports
     */
    @GetMapping
    @PreAuthorize("hasAnyRole('CE_SECRETARY', 'CE_COORDINATOR', 'ADMIN_TENANT', 'SUPER_ADMIN')")
    @Operation(summary = "Lister les rapports annuels", description = "Retourne les rapports annuels paginés du tenant courant")
    public ResponseEntity<Page<AnnualReportResponse>> getReports(Pageable pageable) {
        return ResponseEntity.ok(annualReportService.getReports(pageable));
    }

    /**
     * Retourne les rapports annuels dus dans les prochains X jours.
     *
     * @param daysAhead nombre de jours à l'avance (défaut: 60)
     * @return 200 OK avec la liste des rapports dus prochainement
     */
    @GetMapping("/due")
    @PreAuthorize("hasAnyRole('CE_SECRETARY', 'CE_COORDINATOR', 'ADMIN_TENANT', 'SUPER_ADMIN')")
    @Operation(summary = "Rapports dus", description = "Retourne les rapports annuels dont l'échéance approche")
    public ResponseEntity<List<AnnualReportResponse>> getDueReports(
        @RequestParam(defaultValue = "60") int daysAhead
    ) {
        return ResponseEntity.ok(annualReportService.getDueReports(daysAhead));
    }

    /**
     * Crée un nouveau rapport annuel attendu pour une étude.
     *
     * @param studyId  identifiant de l'étude
     * @param year     année du rapport
     * @param dueDate  date d'échéance pour la soumission
     * @return 201 Created avec le rapport créé
     */
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasAnyRole('CE_SECRETARY', 'CE_COORDINATOR', 'ADMIN_TENANT', 'SUPER_ADMIN')")
    @Operation(summary = "Créer un rapport annuel", description = "Crée un rapport annuel attendu pour une étude")
    public ResponseEntity<AnnualReportResponse> createReport(
        @RequestParam UUID studyId,
        @RequestParam int year,
        @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dueDate
    ) {
        AnnualReportResponse response = annualReportService.createReport(studyId, year, dueDate);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    /**
     * Marque un rapport annuel comme reçu par le secrétariat CE.
     *
     * @param id           identifiant du rapport
     * @param receivedDate date de réception effective
     * @return 200 OK avec le rapport mis à jour
     */
    @PatchMapping("/{id}/received")
    @PreAuthorize("hasAnyRole('CE_SECRETARY', 'CE_COORDINATOR', 'ADMIN_TENANT', 'SUPER_ADMIN')")
    @Operation(summary = "Marquer comme reçu", description = "Enregistre la réception d'un rapport annuel")
    public ResponseEntity<AnnualReportResponse> markReceived(
        @PathVariable UUID id,
        @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate receivedDate
    ) {
        return ResponseEntity.ok(annualReportService.markReceived(id, receivedDate));
    }

    /**
     * Déclenche manuellement la vérification et l'envoi des rappels de rapports annuels.
     *
     * <p>Endpoint batch utilisable par les administrateurs ou dans un contexte de CI/CD.
     * Normalement exécuté automatiquement chaque jour à 8h.
     *
     * @return 202 Accepted
     */
    @PostMapping("/check-due")
    @PreAuthorize("hasAnyRole('CE_COORDINATOR', 'ADMIN_TENANT', 'SUPER_ADMIN')")
    @Operation(summary = "Vérifier les rappels", description = "Déclenche manuellement la vérification des rappels de rapports annuels")
    public ResponseEntity<Void> checkAndSendReminders() {
        log.info("Vérification manuelle des rappels déclenchée via API");
        annualReportService.checkAndSendReminders();
        return ResponseEntity.accepted().build();
    }
}
