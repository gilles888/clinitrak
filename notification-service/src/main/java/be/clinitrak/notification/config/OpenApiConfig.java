package be.clinitrak.notification.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Configuration OpenAPI 3 pour le notification-service.
 */
@Configuration
public class OpenApiConfig {

    /**
     * Configure l'instance OpenAPI avec les informations du service.
     *
     * @return instance OpenAPI configurée
     */
    @Bean
    public OpenAPI notificationServiceOpenAPI() {
        final String securitySchemeName = "bearerAuth";
        return new OpenAPI()
                .info(new Info()
                        .title("CliniTrak — Notification Service API")
                        .description("Service de notifications email et SSE temps réel. " +
                                "Supporte le retry automatique sur les envois email, les templates Thymeleaf " +
                                "et la diffusion d'événements SSE aux clients connectés.")
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
