package be.clinitrak.ethics.service;

import be.clinitrak.ethics.domain.entity.AnnualReport;
import be.clinitrak.ethics.domain.enums.AnnualReportStatus;
import be.clinitrak.ethics.domain.repository.AnnualReportRepository;
import be.clinitrak.ethics.dto.AnnualReportResponse;
import be.clinitrak.ethics.exception.EthicsException;
import be.clinitrak.ethics.exception.EthicsNotFoundException;
import be.clinitrak.ethics.mapper.EthicsMapper;
import be.clinitrak.ethics.tenant.TenantContext;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Service métier pour la gestion des rapports annuels attendus par le Comité d'Éthique.
 *
 * <p>Des rappels automatiques sont envoyés à J-60, J-30 et J-0 (date dépassée)
 * via une tâche planifiée {@link #checkAndSendReminders()} s'exécutant chaque jour à 8h.
 */
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AnnualReportService {

    private final AnnualReportRepository annualReportRepository;
    private final EthicsMapper ethicsMapper;

    /**
     * Retourne les rapports annuels d'un tenant paginés, triés par date d'échéance croissante.
     *
     * @param pageable paramètres de pagination
     * @return page de réponses de rapports annuels
     * @throws EthicsException si le tenant courant n'est pas résolu
     */
    public Page<AnnualReportResponse> getReports(Pageable pageable) {
        UUID tenantId = resolveTenantId();
        return annualReportRepository.findByTenantIdOrderByDueDateAsc(tenantId, pageable)
            .map(this::toResponseWithDaysUntilDue);
    }

    /**
     * Retourne les rapports annuels dus dans les prochains X jours pour le tenant courant.
     *
     * @param daysAhead nombre de jours à l'avance (ex: 60 pour J-60)
     * @return liste des rapports dus dans la période, triée par date d'échéance
     * @throws EthicsException si le tenant courant n'est pas résolu
     */
    public List<AnnualReportResponse> getDueReports(int daysAhead) {
        UUID tenantId = resolveTenantId();
        LocalDate dueBefore = LocalDate.now().plusDays(daysAhead);
        return annualReportRepository.findDueReportsByTenant(tenantId, dueBefore)
            .stream()
            .map(this::toResponseWithDaysUntilDue)
            .collect(Collectors.toList());
    }

    /**
     * Crée un rapport annuel attendu pour une étude donnée.
     *
     * @param studyId  identifiant de l'étude
     * @param year     année du rapport
     * @param dueDate  date d'échéance pour la soumission
     * @return réponse complète du rapport créé
     * @throws EthicsException si un rapport existe déjà pour cette étude/année/tenant
     */
    @Transactional
    public AnnualReportResponse createReport(UUID studyId, int year, LocalDate dueDate) {
        UUID tenantId = resolveTenantId();

        if (annualReportRepository.existsByStudyIdAndReportYearAndTenantId(studyId, year, tenantId)) {
            throw new EthicsException("Un rapport annuel existe déjà pour l'étude " + studyId + " en " + year);
        }

        AnnualReport report = new AnnualReport();
        report.setTenantId(tenantId);
        report.setStudyId(studyId);
        report.setReportYear(year);
        report.setDueDate(dueDate);
        report.setStatus(AnnualReportStatus.PENDING);

        AnnualReport saved = annualReportRepository.save(report);
        log.info("Rapport annuel {} créé pour étude {} (échéance: {}, tenant: {})",
            year, studyId, dueDate, tenantId);

        return toResponseWithDaysUntilDue(saved);
    }

    /**
     * Marque un rapport annuel comme reçu par le secrétariat CE.
     *
     * @param reportId     identifiant du rapport
     * @param receivedDate date de réception effective
     * @return réponse mise à jour du rapport
     * @throws EthicsNotFoundException si le rapport n'existe pas
     * @throws EthicsException         si le tenant courant n'est pas résolu
     */
    @Transactional
    public AnnualReportResponse markReceived(UUID reportId, LocalDate receivedDate) {
        UUID tenantId = resolveTenantId();
        AnnualReport report = annualReportRepository.findById(reportId)
            .filter(r -> tenantId.equals(r.getTenantId()) && !r.isDeleted())
            .orElseThrow(() -> new EthicsNotFoundException("Rapport annuel", reportId));

        report.setReceivedDate(receivedDate);
        report.setStatus(AnnualReportStatus.RECEIVED);

        AnnualReport saved = annualReportRepository.save(report);
        log.info("Rapport annuel {} marqué comme reçu le {} (tenant: {})", reportId, receivedDate, tenantId);

        return toResponseWithDaysUntilDue(saved);
    }

    /**
     * Vérifie les rapports annuels en attente et envoie les rappels appropriés.
     *
     * <p>Cette tâche s'exécute automatiquement chaque jour à 8h00.
     * Pour chaque rapport PENDING, elle évalue si un rappel doit être envoyé
     * selon les seuils J-60, J-30 et J-0. En production, les rappels seraient
     * routés vers le notification-service.
     *
     * <p>Les rapports dépassant leur date d'échéance sont automatiquement
     * mis en statut OVERDUE.
     */
    @Scheduled(cron = "0 0 8 * * ?")
    @Transactional
    public void checkAndSendReminders() {
        LocalDate today = LocalDate.now();
        log.info("Vérification des rappels de rapports annuels — {}", today);

        // Rappels J-60 : rapports dont l'échéance est dans 55 à 65 jours
        List<AnnualReport> reports60 = annualReportRepository
            .findByStatusAndDueDateBeforeAndReminderSent60False(
                AnnualReportStatus.PENDING, today.plusDays(61)
            );
        for (AnnualReport report : reports60) {
            long daysUntilDue = ChronoUnit.DAYS.between(today, report.getDueDate());
            if (daysUntilDue >= 55) { // Fenêtre J-60 ± 5 jours
                sendReminder(report, "J-60");
                report.setReminderSent60(true);
                report.setLastReminderDate(today);
                annualReportRepository.save(report);
            }
        }

        // Rappels J-30 : rapports dont l'échéance est dans 25 à 35 jours
        List<AnnualReport> reports30 = annualReportRepository
            .findByStatusAndDueDateBeforeAndReminderSent30False(
                AnnualReportStatus.PENDING, today.plusDays(31)
            );
        for (AnnualReport report : reports30) {
            long daysUntilDue = ChronoUnit.DAYS.between(today, report.getDueDate());
            if (daysUntilDue >= 25) { // Fenêtre J-30 ± 5 jours
                sendReminder(report, "J-30");
                report.setReminderSent30(true);
                report.setLastReminderDate(today);
                annualReportRepository.save(report);
            }
        }

        // Rapports en retard : passage en OVERDUE + rappel J-0
        List<AnnualReport> overdueReports = annualReportRepository
            .findByStatusAndReminderSent0False(AnnualReportStatus.OVERDUE);
        // Également les PENDING dont la date est dépassée
        List<AnnualReport> newlyOverdue = annualReportRepository
            .findByStatusAndDueDateBeforeAndReminderSent60False( // tous PENDING dont date < today
                AnnualReportStatus.PENDING, today
            );

        for (AnnualReport report : newlyOverdue) {
            if (report.getDueDate().isBefore(today)) {
                report.setStatus(AnnualReportStatus.OVERDUE);
                annualReportRepository.save(report);
                log.info("Rapport annuel {} passé en OVERDUE (étude: {}, échéance: {})",
                    report.getId(), report.getStudyId(), report.getDueDate());
            }
        }

        for (AnnualReport report : overdueReports) {
            sendReminder(report, "J-0 (dépassement)");
            report.setReminderSent0(true);
            report.setLastReminderDate(today);
            annualReportRepository.save(report);
        }

        log.info("Vérification terminée — J-60: {}, J-30: {}, J-0: {}",
            reports60.size(), reports30.size(), overdueReports.size());
    }

    // ----------------------------------------------------------------
    // Méthodes utilitaires privées
    // ----------------------------------------------------------------

    /**
     * Envoie un rappel pour un rapport annuel (log en dev, notification-service en production).
     *
     * @param report    rapport annuel concerné
     * @param threshold seuil du rappel (ex: "J-60", "J-30", "J-0")
     */
    private void sendReminder(AnnualReport report, String threshold) {
        log.info("RAPPEL {} — Rapport annuel {} pour étude {} (échéance: {}, tenant: {})",
            threshold, report.getId(), report.getStudyId(), report.getDueDate(), report.getTenantId());
        // TODO production : router vers notification-service via Feign
    }

    /**
     * Convertit un rapport annuel en réponse en calculant le nombre de jours restants.
     *
     * @param report entité AnnualReport
     * @return DTO AnnualReportResponse avec daysUntilDue calculé
     */
    private AnnualReportResponse toResponseWithDaysUntilDue(AnnualReport report) {
        long daysUntilDue = ChronoUnit.DAYS.between(LocalDate.now(), report.getDueDate());
        AnnualReportResponse base = ethicsMapper.toAnnualReportResponse(report);
        return new AnnualReportResponse(
            base.id(), base.tenantId(), base.studyId(), base.reportYear(),
            base.dueDate(), base.receivedDate(), base.status(), base.statusLabel(),
            base.reminderSent60(), base.reminderSent30(), base.reminderSent0(),
            base.lastReminderDate(), base.notes(), daysUntilDue
        );
    }

    /**
     * Résout l'UUID du tenant courant depuis le {@link TenantContext}.
     *
     * @return UUID du tenant
     * @throws EthicsException si le tenant n'est pas résolu
     */
    private UUID resolveTenantId() {
        String tenantStr = TenantContext.getTenantId();
        if (tenantStr == null || tenantStr.isBlank()) {
            throw new EthicsException("Tenant non résolu — vérifiez le header X-Tenant-ID ou le token JWT");
        }
        try {
            return UUID.fromString(tenantStr);
        } catch (IllegalArgumentException e) {
            throw new EthicsException("Identifiant de tenant invalide : " + tenantStr);
        }
    }
}
