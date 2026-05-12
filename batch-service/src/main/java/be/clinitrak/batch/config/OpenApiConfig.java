package be.clinitrak.batch.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Configuration OpenAPI 3 pour le batch-service.
 */
@Configuration
public class OpenApiConfig {

    /**
     * Configure l'instance OpenAPI du batch-service.
     *
     * @return instance OpenAPI configurée
     */
    @Bean
    public OpenAPI batchServiceOpenAPI() {
        final String securitySchemeName = "bearerAuth";
        return new OpenAPI()
                .info(new Info()
                        .title("CliniTrak — Batch Service API")
                        .description("Service de traitements planifiés (Spring Batch). " +
                                "Gère les rappels nocturnes, les rapports hebdomadaires et la facturation mensuelle.")
                        .version("1.0.0")
                        .contact(new Contact()
                                .name("Équipe CliniTrak")
                                .email("clinitrak@chu.be")))
                .addSecurityItem(new SecurityRequirement().addList(securitySchemeName))
                .components(new Components()
                        .addSecuritySchemes(securitySchemeName, new SecurityScheme()
                                .name(securitySchemeName)
                                .type(SecurityScheme.Type.HTTP)
                                .scheme("bearer")
                                .bearerFormat("JWT")));
    }
}
