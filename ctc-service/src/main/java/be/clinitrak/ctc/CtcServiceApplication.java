package be.clinitrak.ctc;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cloud.openfeign.EnableFeignClients;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;

/**
 * Point d'entrée du service CTC (Centre de Thérapie Cellulaire) CliniTrak.
 *
 * <p>Ce service gère :
 * <ul>
 *   <li>Les demandes desk CTC (académique et commercial)</li>
 *   <li>Les visites de monitoring par les CRA</li>
 *   <li>Les contrats et conventions financières</li>
 *   <li>Les demandes d'analyses statistiques</li>
 *   <li>Les études dont le CUSL est promoteur (sponsor CUSL)</li>
 *   <li>Les événements qualité (déviations, EIG, CAPA, audits)</li>
 *   <li>La timeline chronologique des études</li>
 * </ul>
 *
 * <p>Port par défaut : {@code 8084}. Base de données : {@code clinitrak_ctc}.
 */
@SpringBootApplication
@EnableJpaAuditing(auditorAwareRef = "auditorProvider")
@EnableFeignClients(basePackages = "be.clinitrak.ctc.client")
@EnableCaching
public class CtcServiceApplication {

    /**
     * Démarre l'application Spring Boot CTC Service.
     *
     * @param args arguments de la ligne de commande
     */
    public static void main(String[] args) {
        SpringApplication.run(CtcServiceApplication.class, args);
    }
}
