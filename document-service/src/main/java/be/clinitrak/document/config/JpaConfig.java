package be.clinitrak.document.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;

/**
 * Configuration JPA du document-service.
 * Active l'auditing automatique pour les champs {@code createdAt} et {@code updatedAt}.
 */
@Configuration
@EnableJpaAuditing
public class JpaConfig {}
