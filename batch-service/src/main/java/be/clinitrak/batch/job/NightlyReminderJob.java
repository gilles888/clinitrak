package be.clinitrak.batch.job;

import be.clinitrak.batch.feign.NotificationClient;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.StepContribution;
import org.springframework.batch.core.job.builder.JobBuilder;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.scope.context.ChunkContext;
import org.springframework.batch.core.step.builder.StepBuilder;
import org.springframework.batch.core.step.tasklet.Tasklet;
import org.springframework.batch.repeat.RepeatStatus;
import org.springframework.context.annotation.Bean;
import org.springframework.stereotype.Component;
import org.springframework.transaction.PlatformTransactionManager;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Job Spring Batch de rappel nocturne.
 * Exécuté chaque nuit à 2h00 pour envoyer des rappels aux investigateurs
 * dont les rapports annuels ou les soumissions sont imminentes.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class NightlyReminderJob {

    private final NotificationClient notificationClient;
    private final JobRepository jobRepository;
    private final PlatformTransactionManager transactionManager;

    /** Nom du job utilisé pour le déclenchement manuel. */
    public static final String JOB_NAME = "nightlyReminderJob";

    /**
     * Définit le job Spring Batch de rappel nocturne.
     *
     * @return Job Spring Batch configuré
     */
    @Bean(name = JOB_NAME)
    public Job nightlyReminderJob() {
        return new JobBuilder(JOB_NAME, jobRepository)
                .start(nightlyReminderStep())
                .build();
    }

    /**
     * Définit l'étape (step) principale du job de rappel nocturne.
     *
     * @return Step Spring Batch configuré
     */
    @Bean
    public Step nightlyReminderStep() {
        return new StepBuilder("nightlyReminderStep", jobRepository)
                .tasklet(nightlyReminderTasklet(), transactionManager)
                .build();
    }

    /**
     * Tasklet envoyant les rappels de rapport annuel via le notification-service.
     * En production, ce tasklet consultera le study-service pour obtenir
     * la liste des études dont le rapport annuel est dû dans les 30 jours.
     *
     * @return Tasklet de rappel nocturne
     */
    @Bean
    public Tasklet nightlyReminderTasklet() {
        return (StepContribution contribution, ChunkContext chunkContext) -> {
            log.info("[BATCH] Démarrage du job de rappel nocturne");

            try {
                // Simulation : envoie une notification de rappel rapport annuel
                // En production, appelle le study-service pour obtenir les études éligibles
                Map<String, Object> notificationRequest = new HashMap<>();
                notificationRequest.put("recipientEmail", "investigator@hopital.be");
                notificationRequest.put("recipientUserId", UUID.randomUUID().toString());
                notificationRequest.put("type", "ANNUAL_REPORT_DUE");
                notificationRequest.put("subject", "[CliniTrak] Rappel : rapport annuel à soumettre");
                notificationRequest.put("templateName", "annual-report-due");

                Map<String, Object> templateVars = new HashMap<>();
                templateVars.put("studyTitle", "Étude de référence");
                templateVars.put("studyReference", "PROTO-2026-001");
                templateVars.put("deadline", "31/12/2026");
                templateVars.put("daysRemaining", "30");
                notificationRequest.put("templateVars", templateVars);

                notificationClient.sendNotification(notificationRequest);
                log.info("[BATCH] Rappel nocturne envoyé avec succès");

            } catch (Exception ex) {
                log.error("[BATCH] Erreur lors de l'envoi du rappel nocturne : {}", ex.getMessage());
                // Ne pas faire échouer le job pour une erreur de notification
            }

            contribution.incrementWriteCount(1);
            return RepeatStatus.FINISHED;
        };
    }
}
