package be.clinitrak.pharmacy;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.openfeign.EnableFeignClients;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * Point d'entrée du service de gestion de la pharmacie hospitalière CliniTrak.
 *
 * <p>Ce service gère :
 * <ul>
 *   <li>Les médicaments expérimentaux (IMP/NIMP/Placebo) et leur statut réglementaire</li>
 *   <li>La gestion des stocks de médicaments (quarantaine, disponibilité, péremption)</li>
 *   <li>Les dispensations aux patients avec décrémentation automatique du stock</li>
 *   <li>Les alertes automatiques (stock faible, péremption imminente)</li>
 *   <li>Le levée de l'insu (emergency unblinding) avec double validation</li>
 *   <li>L'import CSV/Excel de stocks via Apache POI</li>
 *   <li>La génération de rapports d'inventaire PDF via Flying Saucer + OpenPDF</li>
 * </ul>
 *
 * <p>Port par défaut : {@code 8085}. Base de données : {@code clinitrak_pharmacy}.
 */
@SpringBootApplication
@EnableJpaAuditing(auditorAwareRef = "auditorProvider")
@EnableFeignClients(basePackages = "be.clinitrak.pharmacy.client")
@EnableAsync
@EnableScheduling
public class PharmacyServiceApplication {

    /**
     * Démarre l'application Spring Boot.
     *
     * @param args arguments de la ligne de commande
     */
    public static void main(String[] args) {
        SpringApplication.run(PharmacyServiceApplication.class, args);
    }
}
