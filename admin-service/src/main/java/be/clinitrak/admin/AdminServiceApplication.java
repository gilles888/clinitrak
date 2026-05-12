package be.clinitrak.admin;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;
import org.springframework.scheduling.annotation.EnableAsync;

/**
 * Point d'entrée du service d'administration CliniTrak.
 *
 * <p>Ce service gère :
 * <ul>
 *   <li>La gestion des tenants (création, configuration, activation des modules)</li>
 *   <li>L'invitation des utilisateurs dans les tenants</li>
 *   <li>Les journaux d'audit système avec export Excel</li>
 *   <li>La santé globale de la plateforme</li>
 * </ul>
 *
 * <p>Port par défaut : {@code 8091}. Base de données : {@code clinitrak_admin}.
 */
@SpringBootApplication
@EnableJpaAuditing(auditorAwareRef = "auditorProvider")
@EnableAsync
public class AdminServiceApplication {

    /**
     * Démarre l'application Spring Boot.
     *
     * @param args arguments de la ligne de commande
     */
    public static void main(String[] args) {
        SpringApplication.run(AdminServiceApplication.class, args);
    }
}
