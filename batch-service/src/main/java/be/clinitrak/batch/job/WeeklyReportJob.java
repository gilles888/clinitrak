package be.clinitrak.batch.job;

import be.clinitrak.batch.feign.NotificationClient;
import be.clinitrak.batch.feign.PharmacyClient;
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
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Job Spring Batch de rapport hebdomadaire.
 * Exécuté chaque lundi à 6h00 pour traiter les alertes de stock de la semaine
 * et notifier les pharmaciens.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class WeeklyReportJob {

    private final PharmacyClient pharmacyClient;
    private final NotificationClient notificationClient;
    private final JobRepository jobRepository;
    private final PlatformTransactionManager transactionManager;

    /** Nom du job utilisé pour le déclenchement manuel. */
    public static final String JOB_NAME = "weeklyReportJob";

    /**
     * Définit le job Spring Batch de rapport hebdomadaire.
     *
     * @return Job Spring Batch configuré
     */
    @Bean(name = JOB_NAME)
    public Job weeklyReportJob() {
        return new JobBuilder(JOB_NAME, jobRepository)
                .start(weeklyReportStep())
                .build();
    }

    /**
     * Définit l'étape principale du rapport hebdomadaire.
     *
     * @return Step Spring Batch configuré
     */
    @Bean
    public Step weeklyReportStep() {
        return new StepBuilder("weeklyReportStep", jobRepository)
                .tasklet(weeklyReportTasklet(), transactionManager)
                .build();
    }

    /**
     * Tasklet qui consulte le pharmacy-service pour les alertes de stock
     * et envoie une notification hebdomadaire aux pharmaciens.
     *
     * @return Tasklet du rapport hebdomadaire
     */
    @Bean
    public Tasklet weeklyReportTasklet() {
        return (StepContribution contribution, ChunkContext chunkContext) -> {
            log.info("[BATCH] Démarrage du job de rapport hebdomadaire");

            int alertCount = 0;
            try {
                List<Map<String, Object>> lowStockAlerts = pharmacyClient.getLowStockAlerts();
                alertCount = lowStockAlerts.size();
                log.info("[BATCH] {} alerte(s) de stock récupérée(s)", alertCount);

                if (alertCount > 0) {
                    Map<String, Object> notificationRequest = new HashMap<>();
                    notificationRequest.put("recipientEmail", "pharmacien@hopital.be");
                    notificationRequest.put("recipientUserId", UUID.randomUUID().toString());
                    notificationRequest.put("type", "STOCK_ALERT");
                    notificationRequest.put("subject",
                            String.format("[CliniTrak] Rapport hebdomadaire : %d alerte(s) de stock", alertCount));
                    notificationRequest.put("templateName", "stock-alert");

                    Map<String, Object> templateVars = new HashMap<>();
                    templateVars.put("alertCount", alertCount);
                    templateVars.put("alerts", lowStockAlerts);
                    notificationRequest.put("templateVars", templateVars);

                    notificationClient.sendNotification(notificationRequest);
                    log.info("[BATCH] Notification d'alerte de stock envoyée");
                }

            } catch (Exception ex) {
                log.error("[BATCH] Erreur lors du rapport hebdomadaire : {}", ex.getMessage());
            }

            contribution.incrementWriteCount(alertCount);
            return RepeatStatus.FINISHED;
        };
    }
}
