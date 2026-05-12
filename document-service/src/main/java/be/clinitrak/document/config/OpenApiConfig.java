package be.clinitrak.document.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Configuration OpenAPI 3 pour le document-service.
 * Expose la documentation Swagger avec authentification JWT Bearer.
 */
@Configuration
public class OpenApiConfig {

    /**
     * Configure l'instance OpenAPI avec les informations du service
     * et le schéma de sécurité JWT Bearer.
     *
     * @return instance OpenAPI configurée
     */
    @Bean
    public OpenAPI documentServiceOpenAPI() {
        final String securitySchemeName = "bearerAuth";
        return new OpenAPI()
                .info(new Info()
                        .title("CliniTrak — Document Service API")
                        .description("Service de gestion documentaire (GED) avec stockage MinIO. " +
                                "Supporte l'upload, le versioning, la génération PDF et les URLs présignées.")
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
