package be.clinitrak.batch.controller;

import be.clinitrak.batch.dto.JobHistoryResponse;
import be.clinitrak.batch.dto.JobTriggerResponse;
import be.clinitrak.batch.job.MonthlyBillingJob;
import be.clinitrak.batch.job.NightlyReminderJob;
import be.clinitrak.batch.job.WeeklyReportJob;
import be.clinitrak.batch.scheduler.BatchScheduler;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.core.*;
import org.springframework.batch.core.explore.JobExplorer;
import org.springframework.batch.core.launch.JobLauncher;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Contrôleur REST du batch-service.
 * Permet le déclenchement manuel des jobs et la consultation de leur historique.
 */
@Slf4j
@RestController
@RequestMapping("/api/v1/batch")
@RequiredArgsConstructor
@Tag(name = "Batch Jobs", description = "Gestion et monitoring des jobs Spring Batch planifiés")
public class BatchController {

    private final JobLauncher jobLauncher;
    private final JobExplorer jobExplorer;
    private final BatchScheduler batchScheduler;

    @Qualifier(NightlyReminderJob.JOB_NAME)
    private final Job nightlyReminderJob;

    @Qualifier(WeeklyReportJob.JOB_NAME)
    private final Job weeklyReportJob;

    @Qualifier(MonthlyBillingJob.JOB_NAME)
    private final Job monthlyBillingJob;

    /** Mapping des noms de jobs vers leurs instances Spring Batch. */
    private static final Set<String> VALID_JOB_NAMES = Set.of(
            NightlyReminderJob.JOB_NAME,
            WeeklyReportJob.JOB_NAME,
            MonthlyBillingJob.JOB_NAME);

    /**
     * Déclenche manuellement un job Spring Batch par son nom.
     *
     * @param jobName nom du job à déclencher
     * @return 200 avec le statut de l'exécution déclenchée
     */
    @PostMapping("/jobs/{jobName}/trigger")
    @Operation(summary = "Déclencher un job manuellement",
               description = "Noms valides : nightlyReminderJob, weeklyReportJob, monthlyBillingJob")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN_TENANT')")
    public ResponseEntity<JobTriggerResponse> triggerJob(
            @Parameter(description = "Nom du job à déclencher") @PathVariable String jobName) {

        if (!VALID_JOB_NAMES.contains(jobName)) {
            return ResponseEntity.badRequest().body(
                    new JobTriggerResponse(jobName, null, "INVALID_JOB",
                            LocalDateTime.now(), "Job inconnu : " + jobName));
        }

        Job targetJob = resolveJob(jobName);
        log.info("Déclenchement manuel du job '{}' par un administrateur", jobName);

        try {
            JobParameters params = new JobParametersBuilder()
                    .addLong("run.id", System.currentTimeMillis())
                    .addString("trigger", "MANUAL")
                    .toJobParameters();

            JobExecution execution = jobLauncher.run(targetJob, params);

            return ResponseEntity.ok(new JobTriggerResponse(
                    jobName,
                    execution.getId(),
                    execution.getStatus().name(),
                    execution.getStartTime() != null
                            ? execution.getStartTime() : LocalDateTime.now(),
                    "Job déclenché avec succès"));

        } catch (Exception ex) {
            log.error("Erreur lors du déclenchement manuel du job '{}' : {}", jobName, ex.getMessage(), ex);
            return ResponseEntity.internalServerError().body(
                    new JobTriggerResponse(jobName, null, "FAILED",
                            LocalDateTime.now(), "Erreur : " + ex.getMessage()));
        }
    }

    /**
     * Retourne l'historique des exécutions de tous les jobs Spring Batch.
     *
     * @return liste des 50 dernières exécutions pour chaque job
     */
    @GetMapping("/jobs/history")
    @Operation(summary = "Historique des exécutions", description = "50 dernières exécutions par job")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN_TENANT')")
    public ResponseEntity<List<JobHistoryResponse>> getJobHistory() {
        List<JobHistoryResponse> history = new ArrayList<>();

        for (String jobName : VALID_JOB_NAMES) {
            List<JobInstance> instances = jobExplorer.findJobInstancesByJobName(jobName, 0, 10);
            for (JobInstance instance : instances) {
                List<JobExecution> executions = jobExplorer.getJobExecutions(instance);
                for (JobExecution execution : executions) {
                    history.add(new JobHistoryResponse(
                            execution.getId(),
                            jobName,
                            execution.getStatus().name(),
                            execution.getStartTime() != null
                                    ? execution.getStartTime() : null,
                            execution.getEndTime() != null
                                    ? execution.getEndTime() : null,
                            execution.getExitStatus().getExitCode(),
                            execution.getExitStatus().getExitDescription()));
                }
            }
        }

        history.sort((a, b) -> {
            if (a.startTime() == null && b.startTime() == null) return 0;
            if (a.startTime() == null) return 1;
            if (b.startTime() == null) return -1;
            return b.startTime().compareTo(a.startTime());
        });

        return ResponseEntity.ok(history);
    }

    /**
     * Retourne le statut actuel de chaque job (dernière exécution).
     *
     * @return map job → statut de la dernière exécution
     */
    @GetMapping("/jobs/status")
    @Operation(summary = "Statut actuel des jobs", description = "Dernière exécution de chaque job planifié")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN_TENANT')")
    public ResponseEntity<Map<String, String>> getJobStatus() {
        Map<String, String> statuses = new java.util.LinkedHashMap<>();

        for (String jobName : VALID_JOB_NAMES) {
            List<JobInstance> instances = jobExplorer.findJobInstancesByJobName(jobName, 0, 1);
            if (instances.isEmpty()) {
                statuses.put(jobName, "NEVER_RUN");
            } else {
                List<JobExecution> executions = jobExplorer.getJobExecutions(instances.get(0));
                if (executions.isEmpty()) {
                    statuses.put(jobName, "NEVER_RUN");
                } else {
                    statuses.put(jobName, executions.get(0).getStatus().name());
                }
            }
        }

        return ResponseEntity.ok(statuses);
    }

    /**
     * Résout l'instance de Job à partir de son nom.
     */
    private Job resolveJob(String jobName) {
        return switch (jobName) {
            case NightlyReminderJob.JOB_NAME -> nightlyReminderJob;
            case WeeklyReportJob.JOB_NAME -> weeklyReportJob;
            case MonthlyBillingJob.JOB_NAME -> monthlyBillingJob;
            default -> throw new IllegalArgumentException("Job inconnu : " + jobName);
        };
    }
}
