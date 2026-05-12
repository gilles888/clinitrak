package be.clinitrak.admin.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.domain.AuditorAware;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.Optional;

/**
 * Configuration du fournisseur d'auditeur pour Spring Data JPA Auditing.
 *
 * <p>Fournit l'identité de l'utilisateur courant (email extrait du JWT)
 * pour alimenter les champs {@code createdBy} et {@code updatedBy} des entités.
 */
@Configuration
public class AuditorConfig {

    /**
     * Fournit l'auditeur courant depuis le SecurityContext.
     *
     * <p>Retourne l'email de l'utilisateur authentifié, ou {@code "system"}
     * si aucune authentification n'est disponible (ex: tâches planifiées).
     *
     * @return {@link AuditorAware} résolvant l'email de l'utilisateur courant
     */
    @Bean
    public AuditorAware<String> auditorProvider() {
        return () -> {
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            if (authentication == null || !authentication.isAuthenticated()) {
                return Optional.of("system");
            }
            return Optional.ofNullable(authentication.getName()).or(() -> Optional.of("system"));
        };
    }
}
