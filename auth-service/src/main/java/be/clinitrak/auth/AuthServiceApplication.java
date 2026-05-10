package be.clinitrak.auth;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * Point d'entrée du service d'authentification CliniTrak.
 *
 * <p>Ce service gère :
 * <ul>
 *   <li>L'authentification multi-tenant via JWT (Access 15min + Refresh 7j)</li>
 *   <li>Le RBAC (Role-Based Access Control) par tenant</li>
 *   <li>L'audit des actions utilisateurs</li>
 * </ul>
 */
@SpringBootApplication
@EnableJpaAuditing(auditorAwareRef = "auditorProvider")
@EnableAsync
@EnableScheduling
public class AuthServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(AuthServiceApplication.class, args);
    }
}
