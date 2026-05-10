package be.clinitrak.study.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import io.swagger.v3.oas.models.servers.Server;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

/**
 * Configuration Springdoc OpenAPI 2.x pour le study-service.
 *
 * <p>Expose la documentation Swagger UI sur {@code /swagger-ui.html}
 * et la spec JSON sur {@code /v3/api-docs}.
 */
@Configuration
public class OpenApiConfig {

    @Value("${spring.application.name:study-service}")
    private String appName;

    /**
     * Définit la spécification OpenAPI 3.1 avec sécurité Bearer JWT.
     *
     * @return instance {@link OpenAPI} configurée pour le study-service
     */
    @Bean
    public OpenAPI clinitrakOpenAPI() {
        final String securitySchemeName = "bearerAuth";

        return new OpenAPI()
            .info(new Info()
                .title("CliniTrak — Study Service API")
                .description("Service de gestion des études cliniques pour la plateforme CliniTrak. " +
                    "Gère les protocoles, statuts, contacts, soumissions réglementaires et patients pseudonymisés.")
                .version("1.0.0")
                .contact(new Contact()
                    .name("Équipe CliniTrak")
                    .email("dev@clinitrak.be"))
                .license(new License()
                    .name("Propriétaire — Cliniques Universitaires Saint-Luc")
                    .url("https://www.saintluc.be"))
            )
            .servers(List.of(
                new Server().url("http://localhost:8082").description("Dev local"),
                new Server().url("https://api-staging.clinitrak.be").description("Staging"),
                new Server().url("https://api.clinitrak.be").description("Production")
            ))
            .addSecurityItem(new SecurityRequirement().addList(securitySchemeName))
            .components(new Components()
                .addSecuritySchemes(securitySchemeName, new SecurityScheme()
                    .name(securitySchemeName)
                    .type(SecurityScheme.Type.HTTP)
                    .scheme("bearer")
                    .bearerFormat("JWT")
                    .description("JWT access token obtenu via POST /api/v1/auth/login sur l'auth-service")
                )
            );
    }
}
