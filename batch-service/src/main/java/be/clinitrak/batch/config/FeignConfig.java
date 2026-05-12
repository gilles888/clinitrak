package be.clinitrak.batch.config;

import feign.Logger;
import feign.codec.ErrorDecoder;
import org.springframework.cloud.openfeign.EnableFeignClients;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Configuration Feign pour les clients inter-services du batch-service.
 * Configure le logging et la gestion des erreurs HTTP des appels Feign.
 */
@Configuration
@EnableFeignClients(basePackages = "be.clinitrak.batch.feign")
public class FeignConfig {

    /**
     * Configure le niveau de log Feign en mode BASIC (méthode + URL + statut).
     *
     * @return niveau de log Feign
     */
    @Bean
    public Logger.Level feignLoggerLevel() {
        return Logger.Level.BASIC;
    }

    /**
     * Décodeur d'erreurs Feign par défaut.
     * Retourne les exceptions Spring appropriées selon le code HTTP.
     *
     * @return décodeur d'erreurs
     */
    @Bean
    public ErrorDecoder feignErrorDecoder() {
        return new ErrorDecoder.Default();
    }
}
