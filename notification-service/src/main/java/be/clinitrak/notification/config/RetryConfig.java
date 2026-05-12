package be.clinitrak.notification.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.retry.annotation.EnableRetry;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * Configuration du mécanisme de retry Spring et de l'exécution asynchrone.
 * Active {@code @Retryable} pour l'envoi email et {@code @Async} pour la diffusion SSE.
 */
@Configuration
@EnableRetry
@EnableAsync
@EnableScheduling
public class RetryConfig {}
