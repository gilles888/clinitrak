package be.clinitrak.ctc.client;

import be.clinitrak.ctc.client.dto.StudyBasicInfo;
import be.clinitrak.ctc.config.FeignConfig;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

/**
 * Client Feign pour accéder au study-service depuis le ctc-service.
 *
 * <p>Transmet automatiquement le JWT Bearer token et le header X-Tenant-ID
 * via la configuration {@link FeignConfig}.
 */
@FeignClient(
    name = "study-service",
    url = "${clinitrak.services.study-service.url:http://localhost:8082}",
    configuration = FeignConfig.class
)
public interface StudyServiceClient {

    /**
     * Récupère le résumé d'une étude clinique par son identifiant.
     *
     * @param id identifiant de l'étude (UUID sous forme de String)
     * @return informations de base de l'étude
     */
    @GetMapping("/api/v1/studies/{id}/summary")
    StudyBasicInfo getStudySummary(@PathVariable("id") String id);
}
