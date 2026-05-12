package be.clinitrak.notification;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Point d'entrée du notification-service CliniTrak.
 * Service de notifications email (Spring Mail + Thymeleaf + retry)
 * et temps réel via SSE (Server-Sent Events).
 */
@SpringBootApplication
public class NotificationServiceApplication {

    /**
     * Démarre le notification-service.
     *
     * @param args arguments de ligne de commande
     */
    public static void main(String[] args) {
        SpringApplication.run(NotificationServiceApplication.class, args);
    }
}
