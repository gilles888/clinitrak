package be.clinitrak.batch.scheduler;

import be.clinitrak.batch.job.MonthlyBillingJob;
import be.clinitrak.batch.job.NightlyReminderJob;
import be.clinitrak.batch.job.WeeklyReportJob;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.JobParameters;
import org.springframework.batch.core.JobParametersBuilder;
import org.springframework.batch.core.launch.JobLauncher;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * Planificateur des jobs Spring Batch du batch-service.
 * Chaque méthode déclenche son job selon une expression cron configurée.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class BatchScheduler {

    private final JobLauncher jobLauncher;

    @Qualifier(NightlyReminderJob.JOB_NAME)
    private final Job nightlyReminderJob;

    @Qualifier(WeeklyReportJob.JOB_NAME)
    private final Job weeklyReportJob;

    @Qualifier(MonthlyBillingJob.JOB_NAME)
    private final Job monthlyBillingJob;

    /**
     * Exécute le job de rappel nocturne chaque nuit à 2h00.
     * Envoie des rappels pour les rapports annuels et soumissions imminentes.
     */
    @Scheduled(cron = "0 0 2 * * *")
    public void runNightlyReminder() {
        log.info("[BATCH-SCHEDULER] Déclenchement du job de rappel nocturne");
        launchJob(nightlyReminderJob, NightlyReminderJob.JOB_NAME);
    }

    /**
     * Exécute le job de rapport hebdomadaire chaque lundi à 6h00.
     * Consolide les alertes de stock et envoie le rapport aux pharmaciens.
     */
    @Scheduled(cron = "0 0 6 * * MON")
    public void runWeeklyReport() {
        log.info("[BATCH-SCHEDULER] Déclenchement du job de rapport hebdomadaire");
        launchJob(weeklyReportJob, WeeklyReportJob.JOB_NAME);
    }

    /**
     * Exécute le job de facturation mensuelle le 1er de chaque mois à 3h00.
     * Consolide les activités facturables et notifie l'équipe COFI.
     */
    @Scheduled(cron = "0 0 3 1 * *")
    public void runMonthlyBilling() {
        log.info("[BATCH-SCHEDULER] Déclenchement du job de facturation mensuelle");
        launchJob(monthlyBillingJob, MonthlyBillingJob.JOB_NAME);
    }

    /**
     * Lance un job Spring Batch avec des paramètres d'exécution uniques (timestamp).
     * L'unicité du timestamp garantit que le même job peut être relancé plusieurs fois.
     *
     * @param job     le job Spring Batch à exécuter
     * @param jobName nom du job pour le logging
     */
    public void launchJob(Job job, String jobName) {
        try {
            JobParameters params = new JobParametersBuilder()
                    .addLong("run.id", System.currentTimeMillis())
                    .toJobParameters();
            jobLauncher.run(job, params);
            log.info("[BATCH-SCHEDULER] Job '{}' démarré avec succès", jobName);
        } catch (Exception ex) {
            log.error("[BATCH-SCHEDULER] Erreur lors du démarrage du job '{}' : {}", jobName, ex.getMessage(), ex);
        }
    }
}
