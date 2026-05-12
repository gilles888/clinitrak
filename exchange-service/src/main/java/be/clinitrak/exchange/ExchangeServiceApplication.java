package be.clinitrak.exchange;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;
import org.springframework.scheduling.annotation.EnableAsync;

/**
 * Point d'entrée du service d'échanges externes CliniTrak.
 *
 * <p>Ce service gère :
 * <ul>
 *   <li>L'authentification des utilisateurs externes (firmes, investigateurs)</li>
 *   <li>La soumission de demandes d'échanges vers le CE ou le CTC</li>
 *   <li>La messagerie bidirectionnelle interne/externe</li>
 *   <li>Les documents attachés aux demandes d'échange</li>
 * </ul>
 *
 * <p>Port par défaut : {@code 8086}. Base de données : {@code clinitrak_exchange}.
 */
@SpringBootApplication
@EnableJpaAuditing(auditorAwareRef = "auditorProvider")
@EnableAsync
public class ExchangeServiceApplication {

    /**
     * Démarre l'application Spring Boot.
     *
     * @param args arguments de la ligne de commande
     */
    public static void main(String[] args) {
        SpringApplication.run(ExchangeServiceApplication.class, args);
    }
}
