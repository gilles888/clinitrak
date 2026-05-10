package be.clinitrak.ethics.controller;

import be.clinitrak.ethics.dto.EthicsDashboardResponse;
import be.clinitrak.ethics.service.EthicsDashboardService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Contrôleur REST pour le tableau de bord du Comité d'Éthique.
 *
 * <p>Agrège les indicateurs clés du CE pour la vue d'ensemble du secrétariat.
 */
@Slf4j
@RestController
@RequestMapping("/api/v1/ethics/dashboard")
@RequiredArgsConstructor
@Tag(name = "Tableau de bord CE", description = "Indicateurs clés du Comité d'Éthique")
public class EthicsDashboardController {

    private final EthicsDashboardService dashboardService;

    /**
     * Retourne le tableau de bord CE du tenant courant.
     *
     * <p>Agrège : avis en attente, réunions à venir, rapports annuels dus,
     * répartition des décisions et les 5 derniers avis en attente.
     *
     * @return 200 OK avec les indicateurs du tableau de bord
     */
    @GetMapping
    @PreAuthorize("hasAnyRole('CE_SECRETARY', 'CE_COORDINATOR', 'ADMIN_TENANT', 'SUPER_ADMIN')")
    @Operation(summary = "Tableau de bord", description = "Retourne les indicateurs clés du Comité d'Éthique")
    public ResponseEntity<EthicsDashboardResponse> getDashboard() {
        return ResponseEntity.ok(dashboardService.getDashboard());
    }
}
