package be.clinitrak.ethics.config;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.templatemode.TemplateMode;
import org.thymeleaf.templateresolver.StringTemplateResolver;

/**
 * Configuration Thymeleaf dédiée au traitement des templates stockés en base de données.
 *
 * <p>Un {@link TemplateEngine} séparé est configuré avec un {@link StringTemplateResolver}
 * pour traiter les templates HTML stockés en base, sans dépendre de fichiers sur disque.
 * Cette configuration coexiste avec le TemplateEngine Spring Boot standard.
 */
@Configuration
public class ThymeleafConfig {

    /**
     * TemplateEngine dédié au traitement des templates de correspondance stockés en base.
     *
     * <p>Utilise {@link StringTemplateResolver} pour accepter des strings HTML directement
     * sans résolution de fichier. Le cache est désactivé car les templates peuvent être
     * modifiés en base sans redémarrage.
     *
     * @return {@link TemplateEngine} configuré pour les templates en base de données
     */
    @Bean
    @Qualifier("dbTemplateEngine")
    public TemplateEngine dbTemplateEngine() {
        TemplateEngine engine = new TemplateEngine();
        StringTemplateResolver resolver = new StringTemplateResolver();
        resolver.setTemplateMode(TemplateMode.HTML);
        resolver.setCacheable(false);
        engine.setTemplateResolver(resolver);
        return engine;
    }
}
