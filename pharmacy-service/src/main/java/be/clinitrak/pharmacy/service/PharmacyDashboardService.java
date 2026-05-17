package be.clinitrak.pharmacy.service;

import be.clinitrak.pharmacy.domain.enums.AlertType;
import be.clinitrak.pharmacy.domain.enums.StockStatus;
import be.clinitrak.pharmacy.domain.repository.DispensationRepository;
import be.clinitrak.pharmacy.domain.repository.DrugStockRepository;
import be.clinitrak.pharmacy.domain.repository.EmergencyUnblindingRepository;
import be.clinitrak.pharmacy.domain.repository.InvestigationalDrugRepository;
import be.clinitrak.pharmacy.dto.PharmacyAlert;
import be.clinitrak.pharmacy.dto.PharmacyDashboardResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;

/**
 * Service de tableau de bord pharmacie.
 *
 * <p>Agrège les indicateurs clés de la pharmacie pour un tenant :
 * médicaments, stocks, dispensations, alertes et levées d'insu.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class PharmacyDashboardService {

    private final InvestigationalDrugRepository drugRepo;
    private final DrugStockRepository stockRepo;
    private final DispensationRepository dispensationRepo;
    private final EmergencyUnblindingRepository unblindingRepo;
    private final PharmacyAlertService alertService;

    /** Seuil de stock faible pour l'indicateur général (quantité inférieure à ce seuil). */
    private static final int LOW_STOCK_THRESHOLD = 10;

    /** Seuil critique utilisé pour l'indicateur {@code criticalStock} du widget frontend. */
    private static final int CRITICAL_STOCK_THRESHOLD = 5;

    /**
     * Calcule et retourne les indicateurs du tableau de bord pharmacie.
     *
     * <p>Indicateurs calculés :
     * <ul>
     *   <li>Nombre total de médicaments enregistrés</li>
     *   <li>Nombre de stocks en statut AVAILABLE</li>
     *   <li>Nombre de stocks en dessous du seuil de {@value #LOW_STOCK_THRESHOLD} unités</li>
     *   <li>Nombre de stocks expirant dans les 30 prochains jours</li>
     *   <li>Nombre total de dispensations actives</li>
     *   <li>Nombre de levées d'insu en attente d'approbation</li>
     *   <li>5 alertes urgentes (péremption imminente ou stock faible)</li>
     *   <li>Dispensations effectuées aujourd'hui (widget frontend)</li>
     *   <li>Stocks en dessous du seuil critique de {@value #CRITICAL_STOCK_THRESHOLD} unités (widget frontend)</li>
     * </ul>
     *
     * @param tenantId identifiant du tenant
     * @return DTO du tableau de bord avec tous les indicateurs
     */
    @Transactional(readOnly = true)
    public PharmacyDashboardResponse getDashboard(String tenantId) {
        LocalDate today = LocalDate.now();

        long totalDrugs = drugRepo.findByTenantIdAndDeletedFalse(tenantId).size();
        long availableStocks = stockRepo.findByTenantIdAndStatusAndDeletedFalse(tenantId, StockStatus.AVAILABLE).size();
        long lowStockCount = stockRepo.findLowStock(tenantId, LOW_STOCK_THRESHOLD).size();
        long expiringIn30Days = stockRepo.findByTenantIdAndExpiryDateBeforeAndDeletedFalse(
            tenantId, today.plusDays(31)
        ).size();
        long pendingDispensations = dispensationRepo.findByTenantIdAndDeletedFalse(tenantId).size();
        long openUnblindings = unblindingRepo.findByTenantIdAndDeletedFalse(tenantId).stream()
            .filter(u -> u.getApprovedBy() == null)
            .count();

        List<PharmacyAlert> urgentAlerts = alertService.getAlerts(tenantId).alerts().stream()
            .filter(a -> a.type() == AlertType.EXPIRY_7 || a.type() == AlertType.LOW_STOCK)
            .limit(5)
            .toList();

        // Dispensations du jour (widget frontend)
        long dispensationsToday = dispensationRepo.countByTenantIdAndDispensationDate(tenantId, today);

        // Stocks en dessous du seuil critique (widget frontend — seuil plus sévère que lowStockCount)
        long criticalStock = stockRepo.findLowStock(tenantId, CRITICAL_STOCK_THRESHOLD).size();

        return new PharmacyDashboardResponse(
            totalDrugs, availableStocks, lowStockCount,
            expiringIn30Days, pendingDispensations, openUnblindings, urgentAlerts,
            dispensationsToday, criticalStock
        );
    }
}
