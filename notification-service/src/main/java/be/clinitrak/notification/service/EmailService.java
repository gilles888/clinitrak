package be.clinitrak.notification.service;

import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.mail.MailException;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Recover;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Service;
import org.thymeleaf.context.Context;
import org.thymeleaf.spring6.SpringTemplateEngine;

import java.util.Map;

/**
 * Service d'envoi d'emails via Spring Mail et templates Thymeleaf.
 * Implémente un mécanisme de retry (3 tentatives, délai de 2 secondes)
 * en cas d'échec SMTP.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class EmailService {

    private final JavaMailSender mailSender;
    private final SpringTemplateEngine templateEngine;

    /** Adresse d'expédition par défaut. */
    private static final String FROM_ADDRESS = "noreply@clinitrak.be";

    /** Préfixe des templates email dans les ressources Thymeleaf. */
    private static final String EMAIL_TEMPLATE_PREFIX = "email/";

    /**
     * Envoie un email HTML généré depuis un template Thymeleaf.
     * En cas d'échec SMTP, réessaie jusqu'à 3 fois avec un délai de 2 secondes.
     *
     * @param to           adresse email du destinataire
     * @param subject      sujet de l'email
     * @param templateName nom du template Thymeleaf (sans extension .html)
     * @param vars         variables à injecter dans le template
     * @throws MailException si l'envoi échoue après les 3 tentatives
     */
    @Retryable(
            retryFor = {MailException.class, MessagingException.class},
            maxAttempts = 3,
            backoff = @Backoff(delay = 2000, multiplier = 2.0))
    public void sendEmail(String to, String subject, String templateName, Map<String, Object> vars) {
        log.info("Envoi email : to={}, subject={}, template={}", to, subject, templateName);

        Context context = new Context();
        if (vars != null) {
            vars.forEach(context::setVariable);
        }
        context.setVariable("subject", subject);

        String htmlContent = templateEngine.process(EMAIL_TEMPLATE_PREFIX + templateName, context);

        try {
            MimeMessage mimeMessage = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(mimeMessage, true, "UTF-8");
            helper.setFrom(FROM_ADDRESS);
            helper.setTo(to);
            helper.setSubject(subject);
            helper.setText(htmlContent, true);
            mailSender.send(mimeMessage);
            log.info("Email envoyé avec succès à {}", to);
        } catch (MessagingException ex) {
            throw new MailException("Erreur lors de la composition du message", ex) {};
        }
    }

    /**
     * Méthode de récupération appelée après l'échec des 3 tentatives d'envoi.
     * Log l'erreur définitive sans propager d'exception.
     *
     * @param ex      dernière exception levée
     * @param to      adresse email concernée
     * @param subject sujet de l'email
     * @param templateName nom du template
     * @param vars    variables du template
     */
    @Recover
    public void recoverEmail(Exception ex, String to, String subject,
                              String templateName, Map<String, Object> vars) {
        log.error("Email définitivement échoué après 3 tentatives vers {} (sujet: {}) : {}",
                to, subject, ex.getMessage());
    }

    /**
     * Envoie un email de texte brut (sans template).
     * Utilisé pour les notifications simples ne nécessitant pas de mise en page.
     *
     * @param to      adresse email du destinataire
     * @param subject sujet de l'email
     * @param text    corps textuel de l'email
     */
    @Retryable(
            retryFor = {MailException.class},
            maxAttempts = 3,
            backoff = @Backoff(delay = 2000))
    public void sendPlainTextEmail(String to, String subject, String text) {
        log.info("Envoi email texte brut : to={}", to);
        try {
            MimeMessage mimeMessage = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(mimeMessage, false, "UTF-8");
            helper.setFrom(FROM_ADDRESS);
            helper.setTo(to);
            helper.setSubject(subject);
            helper.setText(text, false);
            mailSender.send(mimeMessage);
        } catch (MessagingException ex) {
            throw new MailException("Erreur lors de la composition du message", ex) {};
        }
    }

    /**
     * Récupération après échec de l'envoi de texte brut.
     */
    @Recover
    public void recoverPlainTextEmail(Exception ex, String to, String subject, String text) {
        log.error("Email texte brut définitivement échoué vers {} : {}", to, ex.getMessage());
    }
}
