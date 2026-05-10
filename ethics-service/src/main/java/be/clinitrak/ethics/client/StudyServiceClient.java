package be.clinitrak.ethics.client;

import be.clinitrak.ethics.client.dto.StudyBasicInfo;
import be.clinitrak.ethics.config.FeignConfig;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

import java.util.UUID;

/**
 * Client Feign pour accéder au study-service.
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
     * Récupère les informations de base d'une étude clinique par son identifiant.
     *
     * @param studyId identifiant UUID de l'étude
     * @return informations de base de l'étude
     */
    @GetMapping("/api/v1/studies/{id}")
    StudyBasicInfo getStudy(@PathVariable("id") UUID studyId);
}
