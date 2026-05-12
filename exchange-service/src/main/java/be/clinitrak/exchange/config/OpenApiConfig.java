package be.clinitrak.exchange.config;

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
 * Configuration Springdoc OpenAPI 2.x pour le exchange-service.
 *
 * <p>Expose la documentation Swagger UI sur {@code /swagger-ui.html}
 * et la spec JSON sur {@code /v3/api-docs}.
 */
@Configuration
public class OpenApiConfig {

    @Value("${spring.application.name:exchange-service}")
    private String appName;

    /**
     * Définit la spécification OpenAPI 3.1 avec sécurité Bearer JWT (interne et externe).
     *
     * @return instance {@link OpenAPI} configurée pour le exchange-service
     */
    @Bean
    public OpenAPI clinitrakOpenAPI() {
        final String internalAuth = "bearerAuth";
        final String externalAuth = "externalBearerAuth";

        return new OpenAPI()
            .info(new Info()
                .title("CliniTrak — Exchange Service API")
                .description("Portail d'échanges externes CliniTrak. Permet aux firmes, investigateurs " +
                    "et demandeurs CE de soumettre des demandes et d'échanger avec les équipes internes.")
                .version("1.0.0")
                .contact(new Contact()
                    .name("Équipe CliniTrak")
                    .email("dev@clinitrak.be"))
                .license(new License()
                    .name("Propriétaire — Cliniques Universitaires Saint-Luc")
                    .url("https://www.saintluc.be"))
            )
            .servers(List.of(
                new Server().url("http://localhost:8086").description("Dev local"),
                new Server().url("https://api-staging.clinitrak.be").description("Staging"),
                new Server().url("https://api.clinitrak.be").description("Production")
            ))
            .addSecurityItem(new SecurityRequirement().addList(internalAuth))
            .components(new Components()
                .addSecuritySchemes(internalAuth, new SecurityScheme()
                    .name(internalAuth)
                    .type(SecurityScheme.Type.HTTP)
                    .scheme("bearer")
                    .bearerFormat("JWT")
                    .description("JWT interne obtenu via POST /api/v1/auth/login sur l'auth-service")
                )
                .addSecuritySchemes(externalAuth, new SecurityScheme()
                    .name(externalAuth)
                    .type(SecurityScheme.Type.HTTP)
                    .scheme("bearer")
                    .bearerFormat("JWT")
                    .description("JWT externe obtenu via POST /api/v1/exchange/auth/login")
                )
            );
    }
}
