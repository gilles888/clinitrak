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
import org.springframework.context.annotation.Configuration;
import org.springframework.transaction.PlatformTransactionManager;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Job Spring Batch de facturation mensuelle.
 * Exécuté le 1er de chaque mois à 3h00 pour consolider les activités du mois écoulé
 * et préparer les données de facturation pour le billing-service.
 */
@Slf4j
@Configuration("monthlyBillingJobConfiguration")
@RequiredArgsConstructor
public class MonthlyBillingJob {

    private final NotificationClient notificationClient;
    private final JobRepository jobRepository;
    private final PlatformTransactionManager transactionManager;

    /** Nom du job utilisé pour le déclenchement manuel. */
    public static final String JOB_NAME = "monthlyBillingJob";

    /**
     * Définit le job Spring Batch de facturation mensuelle.
     *
     * @return Job Spring Batch configuré
     */
    @Bean(name = JOB_NAME)
    public Job monthlyBillingJob() {
        return new JobBuilder(JOB_NAME, jobRepository)
                .start(monthlyBillingStep())
                .build();
    }

    /**
     * Définit l'étape principale de la facturation mensuelle.
     *
     * @return Step Spring Batch configuré
     */
    @Bean
    public Step monthlyBillingStep() {
        return new StepBuilder("monthlyBillingStep", jobRepository)
                .tasklet(monthlyBillingTasklet(), transactionManager)
                .build();
    }

    /**
     * Tasklet de consolidation des données de facturation.
     * En production, ce tasklet consultera le billing-service et le study-service
     * pour agréger les activités facturables du mois.
     *
     * @return Tasklet de facturation mensuelle
     */
    @Bean
    public Tasklet monthlyBillingTasklet() {
        return (StepContribution contribution, ChunkContext chunkContext) -> {
            log.info("[BATCH] Démarrage du job de facturation mensuelle");

            try {
                String period = LocalDate.now().minusMonths(1)
                        .format(DateTimeFormatter.ofPattern("MMMM yyyy"));

                // Simulation : notification à l'équipe COFI pour validation
                Map<String, Object> notificationRequest = new HashMap<>();
                notificationRequest.put("recipientEmail", "cofi@hopital.be");
                notificationRequest.put("recipientUserId", UUID.randomUUID().toString());
                notificationRequest.put("type", "SYSTEM_ALERT");
                notificationRequest.put("subject",
                        String.format("[CliniTrak] Consolidation facturation %s — À valider", period));
                notificationRequest.put("templateName", "system-alert");

                Map<String, Object> templateVars = new HashMap<>();
                templateVars.put("period", period);
                templateVars.put("message",
                        "La consolidation mensuelle de la facturation est disponible pour validation.");
                notificationRequest.put("templateVars", templateVars);

                notificationClient.sendNotification(notificationRequest);
                log.info("[BATCH] Notification de facturation mensuelle envoyée pour la période : {}", period);

            } catch (Exception ex) {
                log.error("[BATCH] Erreur lors de la facturation mensuelle : {}", ex.getMessage());
            }

            contribution.incrementWriteCount(1);
            return RepeatStatus.FINISHED;
        };
    }
}
