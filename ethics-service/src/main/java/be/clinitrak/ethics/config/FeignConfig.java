package be.clinitrak.ethics.config;

import be.clinitrak.ethics.tenant.TenantContext;
import feign.RequestInterceptor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

/**
 * Configuration Feign pour les appels inter-services du ethics-service.
 *
 * <p>Transmet automatiquement le token JWT et le header X-Tenant-ID
 * à tous les appels Feign sortants pour maintenir le contexte de sécurité.
 */
@Configuration
public class FeignConfig {

    /**
     * Intercepteur qui transmet le JWT Bearer token et le tenant aux appels Feign.
     *
     * <p>Récupère le header Authorization de la requête entrante et le propage
     * vers les services appelés. Le tenantId est également transmis via
     * le header {@code X-Tenant-ID}.
     *
     * @return intercepteur de requête Feign
     */
    @Bean
    public RequestInterceptor jwtRequestInterceptor() {
        return requestTemplate -> {
            ServletRequestAttributes attrs = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
            if (attrs != null) {
                String auth = attrs.getRequest().getHeader("Authorization");
                if (auth != null) {
                    requestTemplate.header("Authorization", auth);
                }
            }
            String tenantId = TenantContext.getTenantId();
            if (tenantId != null) {
                requestTemplate.header("X-Tenant-ID", tenantId);
            }
        };
    }
}
