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

    /**
     * Calcule et retourne les indicateurs du tableau de bord pharmacie.
     *
     * @param tenantId identifiant du tenant
     * @return DTO du tableau de bord avec tous les indicateurs
     */
    @Transactional(readOnly = true)
    public PharmacyDashboardResponse getDashboard(String tenantId) {
        long totalDrugs = drugRepo.findByTenantIdAndDeletedFalse(tenantId).size();
        long availableStocks = stockRepo.findByTenantIdAndStatusAndDeletedFalse(tenantId, StockStatus.AVAILABLE).size();
        long lowStockCount = stockRepo.findLowStock(tenantId, 10).size();
        long expiringIn30Days = stockRepo.findByTenantIdAndExpiryDateBeforeAndDeletedFalse(
            tenantId, LocalDate.now().plusDays(31)
        ).size();
        long pendingDispensations = dispensationRepo.findByTenantIdAndDeletedFalse(tenantId).size();
        long openUnblindings = unblindingRepo.findByTenantIdAndDeletedFalse(tenantId).stream()
            .filter(u -> u.getApprovedBy() == null)
            .count();

        List<PharmacyAlert> urgentAlerts = alertService.getAlerts(tenantId).alerts().stream()
            .filter(a -> a.type() == AlertType.EXPIRY_7 || a.type() == AlertType.LOW_STOCK)
            .limit(5)
            .toList();

        return new PharmacyDashboardResponse(
            totalDrugs, availableStocks, lowStockCount,
            expiringIn30Days, pendingDispensations, openUnblindings, urgentAlerts
        );
    }
}
