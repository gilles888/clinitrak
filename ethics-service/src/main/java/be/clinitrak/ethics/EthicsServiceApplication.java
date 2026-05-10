package be.clinitrak.ethics;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.openfeign.EnableFeignClients;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * Point d'entrée du service de gestion du Comité d'Éthique CliniTrak.
 *
 * <p>Ce service gère :
 * <ul>
 *   <li>Les avis éthiques (soumissions, décisions, amendements)</li>
 *   <li>Les réunions du Comité d'Éthique et leur ordre du jour</li>
 *   <li>Les rapports annuels avec rappels automatisés</li>
 *   <li>La correspondance (lettres d'approbation, refus, demandes d'informations)</li>
 *   <li>La génération de PDF via Flying Saucer + OpenPDF</li>
 * </ul>
 *
 * <p>Port par défaut : {@code 8083}. Base de données : {@code clinitrak_ethics}.
 */
@SpringBootApplication
@EnableJpaAuditing(auditorAwareRef = "auditorProvider")
@EnableFeignClients(basePackages = "be.clinitrak.ethics.client")
@EnableAsync
@EnableScheduling
public class EthicsServiceApplication {

    /**
     * Démarre l'application Spring Boot.
     *
     * @param args arguments de la ligne de commande
     */
    public static void main(String[] args) {
        SpringApplication.run(EthicsServiceApplication.class, args);
    }
}
