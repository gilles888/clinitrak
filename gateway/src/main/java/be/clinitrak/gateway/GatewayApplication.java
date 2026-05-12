package be.clinitrak.gateway;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Point d'entrée de l'API Gateway CliniTrak.
 * Configure le routage Spring Cloud Gateway avec validation JWT globale,
 * rate limiting Redis et logging des requêtes.
 */
@SpringBootApplication
public class GatewayApplication {

    /**
     * Démarre l'API Gateway.
     *
     * @param args arguments de ligne de commande
     */
    public static void main(String[] args) {
        SpringApplication.run(GatewayApplication.class, args);
    }
}
