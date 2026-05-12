package be.clinitrak.batch.feign;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

import java.util.Map;
import java.util.UUID;

/**
 * Client Feign vers le notification-service CliniTrak.
 * Utilisé par le batch-service pour envoyer des notifications planifiées.
 */
@FeignClient(name = "notification-service", url = "${clinitrak.services.notification-service.url}")
public interface NotificationClient {

    /**
     * Envoie une notification via le notification-service.
     * Le body est un Map simplifié car le batch n'importe pas les DTOs du notification-service.
     *
     * @param request corps de la requête de notification
     */
    @PostMapping("/api/v1/notifications/send")
    void sendNotification(@RequestBody Map<String, Object> request);
}
