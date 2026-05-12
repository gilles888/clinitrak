package be.clinitrak.batch.feign;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;

import java.util.List;
import java.util.Map;

/**
 * Client Feign vers le pharmacy-service CliniTrak.
 * Utilisé par le batch-service pour récupérer les alertes de stock.
 */
@FeignClient(name = "pharmacy-service", url = "${clinitrak.services.pharmacy-service.url}")
public interface PharmacyClient {

    /**
     * Récupère la liste des médicaments en rupture de stock ou sous le seuil d'alerte.
     * Chaque entrée est une Map contenant les propriétés du médicament en alerte.
     *
     * @return liste des médicaments en alerte de stock
     */
    @GetMapping("/api/v1/pharmacy/medications/alerts")
    List<Map<String, Object>> getLowStockAlerts();
}
