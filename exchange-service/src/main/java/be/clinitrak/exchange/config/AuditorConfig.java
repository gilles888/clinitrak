package be.clinitrak.exchange.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.domain.AuditorAware;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.Optional;

/**
 * Configuration du fournisseur d'auditeur pour Spring Data JPA Auditing.
 *
 * <p>Fournit l'identifiant de l'utilisateur courant (interne ou externe) pour les champs
 * {@code @CreatedBy} et {@code @LastModifiedBy} des entités.
 */
@Configuration
public class AuditorConfig {

    /**
     * Retourne le fournisseur d'auditeur basé sur le SecurityContext.
     *
     * <p>L'identifiant de l'utilisateur courant (email interne ou UUID externe) est
     * extrait du contexte Spring Security.
     *
     * @return {@link AuditorAware} fournissant l'identifiant de l'utilisateur courant
     */
    @Bean
    public AuditorAware<String> auditorProvider() {
        return () -> Optional.ofNullable(SecurityContextHolder.getContext().getAuthentication())
            .filter(auth -> auth.isAuthenticated() && !"anonymousUser".equals(auth.getPrincipal()))
            .map(auth -> {
                String name = auth.getName();
                return name.contains(":") ? name.split(":")[0] : name;
            });
    }
}
