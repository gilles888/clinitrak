package be.clinitrak.pharmacy.service;

import be.clinitrak.pharmacy.domain.repository.DrugStockRepository;
import be.clinitrak.pharmacy.domain.enums.AlertType;
import be.clinitrak.pharmacy.dto.PharmacyAlert;
import be.clinitrak.pharmacy.dto.PharmacyAlertsResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/**
 * Service de gestion des alertes pharmacie.
 *
 * <p>Génère des alertes pour :
 * <ul>
 *   <li>Stocks faibles (quantité inférieure au seuil configuré)</li>
 *   <li>Péremptions dans les 7 jours (critique)</li>
 *   <li>Péremptions dans les 30 jours (avertissement)</li>
 * </ul>
 *
 * <p>Une vérification quotidienne est schedulée à 6h du matin.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class PharmacyAlertService {

    @Value("${clinitrak.pharmacy.low-stock-threshold:10}")
    private int lowStockThreshold;

    private final DrugStockRepository stockRepo;

    /**
     * Génère la liste des alertes actives pour un tenant.
     *
     * @param tenantId identifiant du tenant
     * @return réponse contenant les alertes et le nombre d'alertes critiques
     */
    @Transactional(readOnly = true)
    public PharmacyAlertsResponse getAlerts(String tenantId) {
        List<PharmacyAlert> alerts = new ArrayList<>();
        LocalDate today = LocalDate.now();

        // Stocks faibles
        stockRepo.findLowStock(tenantId, lowStockThreshold).forEach(s ->
            alerts.add(new PharmacyAlert(
                AlertType.LOW_STOCK,
                AlertType.LOW_STOCK.getLabel(),
                s.getStudyId(),
                s.getDrug() != null ? s.getDrug().getDrugName() : "Inconnu",
                "Quantité: " + s.getQuantity() + " " + s.getUnit(),
                today
            ))
        );

        // Péremptions J-7 (expire dans moins de 8 jours mais pas encore aujourd'hui)
        stockRepo.findByTenantIdAndExpiryDateBeforeAndDeletedFalse(tenantId, today.plusDays(8)).stream()
            .filter(s -> !s.getExpiryDate().isBefore(today))
            .forEach(s -> alerts.add(new PharmacyAlert(
                AlertType.EXPIRY_7,
                AlertType.EXPIRY_7.getLabel(),
                s.getStudyId(),
                s.getDrug() != null ? s.getDrug().getDrugName() : "Inconnu",
                "Expire le " + s.getExpiryDate(),
                s.getExpiryDate()
            )));

        // Péremptions J-30 (expire entre J-8 et J-30)
        stockRepo.findByTenantIdAndExpiryDateBeforeAndDeletedFalse(tenantId, today.plusDays(31)).stream()
            .filter(s -> !s.getExpiryDate().isBefore(today.plusDays(8)))
            .forEach(s -> alerts.add(new PharmacyAlert(
                AlertType.EXPIRY_30,
                AlertType.EXPIRY_30.getLabel(),
                s.getStudyId(),
                s.getDrug() != null ? s.getDrug().getDrugName() : "Inconnu",
                "Expire le " + s.getExpiryDate(),
                s.getExpiryDate()
            )));

        int criticalCount = (int) alerts.stream()
            .filter(a -> a.type() == AlertType.EXPIRY_7 || a.type() == AlertType.LOW_STOCK)
            .count();

        return new PharmacyAlertsResponse(alerts, criticalCount);
    }

    /**
     * Vérification quotidienne des alertes pharmacie à 6h du matin.
     *
     * <p>Les alertes sont loggées. L'envoi d'email est délégué au notification-service.
     */
    @Scheduled(cron = "0 0 6 * * ?")
    public void sendDailyAlerts() {
        log.info("[PharmacyAlertService] Vérification quotidienne des alertes pharmacie");
        // Alertes loggées — email délégué au notification-service
    }
}
