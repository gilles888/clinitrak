package be.clinitrak.study;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;
import org.springframework.scheduling.annotation.EnableAsync;

/**
 * Point d'entrée du service de gestion des études cliniques CliniTrak.
 *
 * <p>Ce service gère :
 * <ul>
 *   <li>Le CRUD des études cliniques (protocoles, statuts, contacts)</li>
 *   <li>Le suivi des soumissions aux autorités compétentes</li>
 *   <li>La gestion des patients pseudonymisés (RGPD)</li>
 *   <li>Les statistiques et tableaux de bord par tenant</li>
 * </ul>
 */
@SpringBootApplication
@EnableJpaAuditing(auditorAwareRef = "auditorProvider")
@EnableAsync
public class StudyServiceApplication {

    /**
     * Démarre l'application Spring Boot.
     *
     * @param args arguments de la ligne de commande
     */
    public static void main(String[] args) {
        SpringApplication.run(StudyServiceApplication.class, args);
    }
}
