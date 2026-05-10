package be.clinitrak.ethics.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.domain.AuditorAware;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.Optional;

/**
 * Configuration du fournisseur d'auditeur pour Spring Data JPA Auditing.
 *
 * <p>Fournit l'identifiant de l'utilisateur courant pour les champs
 * {@code @CreatedBy} et {@code @LastModifiedBy} des entités.
 */
@Configuration
public class AuditorConfig {

    /**
     * Retourne le fournisseur d'auditeur basé sur le SecurityContext.
     *
     * <p>L'email de l'utilisateur courant est extrait du contexte Spring Security.
     * Si l'utilisateur n'est pas authentifié, retourne empty (le champ reste null).
     *
     * @return {@link AuditorAware} fournissant l'email de l'utilisateur courant
     */
    @Bean
    public AuditorAware<String> auditorProvider() {
        return () -> Optional.ofNullable(SecurityContextHolder.getContext().getAuthentication())
            .filter(auth -> auth.isAuthenticated() && !"anonymousUser".equals(auth.getPrincipal()))
            .map(auth -> {
                String name = auth.getName();
                // Format interne "email:tenantId" → on extrait l'email si présent
                return name.contains(":") ? name.split(":")[0] : name;
            });
    }
}
