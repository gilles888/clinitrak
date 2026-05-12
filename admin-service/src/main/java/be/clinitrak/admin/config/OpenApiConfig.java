package be.clinitrak.admin.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Configuration OpenAPI 3 / Swagger UI pour le admin-service.
 *
 * <p>Expose la documentation interactive sur {@code /swagger-ui.html}.
 * Tous les endpoints nécessitent un JWT Bearer dans le header Authorization.
 */
@Configuration
public class OpenApiConfig {

    private static final String SECURITY_SCHEME_NAME = "bearerAuth";

    /**
     * Crée la configuration OpenAPI avec schéma de sécurité JWT Bearer.
     *
     * @return instance {@link OpenAPI} configurée
     */
    @Bean
    public OpenAPI adminServiceOpenApi() {
        return new OpenAPI()
            .info(new Info()
                .title("CliniTrak — Admin Service API")
                .description("API d'administration de la plateforme CliniTrak : gestion des tenants, utilisateurs et audit système")
                .version("1.0.0")
                .contact(new Contact()
                    .name("CliniTrak Team")
                    .email("support@clinitrak.be")))
            .addSecurityItem(new SecurityRequirement().addList(SECURITY_SCHEME_NAME))
            .components(new Components()
                .addSecuritySchemes(SECURITY_SCHEME_NAME,
                    new SecurityScheme()
                        .name(SECURITY_SCHEME_NAME)
                        .type(SecurityScheme.Type.HTTP)
                        .scheme("bearer")
                        .bearerFormat("JWT")
                        .description("JWT émis par l'auth-service. Rôle requis : ROLE_SUPER_ADMIN")));
    }
}
