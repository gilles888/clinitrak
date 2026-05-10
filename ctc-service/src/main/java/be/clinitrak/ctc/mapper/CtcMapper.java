package be.clinitrak.ctc.mapper;

import be.clinitrak.ctc.domain.entity.*;
import be.clinitrak.ctc.domain.enums.*;
import be.clinitrak.ctc.dto.*;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

/**
 * Mapper MapStruct pour la conversion entre entités JPA et DTOs du ctc-service.
 *
 * <p>MapStruct génère l'implémentation à la compilation.
 * Le composant est enregistré comme bean Spring via {@code componentModel = "spring"}.
 *
 * <p>Les champs {@code createdAt} sont de type {@code Instant} en base mais
 * les DTOs utilisent {@code LocalDateTime} — la conversion est gérée via
 * des expressions de mapping explicites.
 */
@Mapper(componentModel = "spring")
public interface CtcMapper {

    // ----------------------------------------------------------------
    // TrialDeskRequest
    // ----------------------------------------------------------------

    /**
     * Convertit une demande desk en réponse avec libellés des enums.
     *
     * @param entity entité TrialDeskRequest
     * @return DTO TrialDeskRequestResponse
     */
    @Mapping(target = "deskTypeLabel",    expression = "java(deskTypeLabel(entity.getDeskType()))")
    @Mapping(target = "requestTypeLabel", expression = "java(requestTypeLabel(entity.getRequestType()))")
    @Mapping(target = "statusLabel",      expression = "java(requestStatusLabel(entity.getStatus()))")
    @Mapping(target = "priorityLabel",    expression = "java(priorityLabel(entity.getPriority()))")
    @Mapping(target = "createdAt",        expression = "java(entity.getCreatedAt() != null ? java.time.LocalDateTime.ofInstant(entity.getCreatedAt(), java.time.ZoneOffset.UTC) : null)")
    TrialDeskRequestResponse toResponse(TrialDeskRequest entity);

    // ----------------------------------------------------------------
    // MonitoringVisit
    // ----------------------------------------------------------------

    /**
     * Convertit une visite de monitoring en réponse avec libellés des enums.
     *
     * @param entity entité MonitoringVisit
     * @return DTO MonitoringVisitResponse
     */
    @Mapping(target = "visitTypeLabel", expression = "java(visitTypeLabel(entity.getVisitType()))")
    @Mapping(target = "statusLabel",    expression = "java(visitStatusLabel(entity.getStatus()))")
    @Mapping(target = "createdAt",      expression = "java(entity.getCreatedAt() != null ? java.time.LocalDateTime.ofInstant(entity.getCreatedAt(), java.time.ZoneOffset.UTC) : null)")
    MonitoringVisitResponse toResponse(MonitoringVisit entity);

    // ----------------------------------------------------------------
    // FinancialContract
    // ----------------------------------------------------------------

    /**
     * Convertit un contrat financier en réponse avec libellés des enums.
     *
     * @param entity entité FinancialContract
     * @return DTO FinancialContractResponse
     */
    @Mapping(target = "contractTypeLabel",    expression = "java(contractTypeLabel(entity.getContractType()))")
    @Mapping(target = "currencyLabel",        expression = "java(currencyLabel(entity.getCurrency()))")
    @Mapping(target = "billingScheduleLabel", expression = "java(billingScheduleLabel(entity.getBillingSchedule()))")
    @Mapping(target = "statusLabel",          expression = "java(contractStatusLabel(entity.getStatus()))")
    @Mapping(target = "createdAt",            expression = "java(entity.getCreatedAt() != null ? java.time.LocalDateTime.ofInstant(entity.getCreatedAt(), java.time.ZoneOffset.UTC) : null)")
    FinancialContractResponse toResponse(FinancialContract entity);

    // ----------------------------------------------------------------
    // StatisticsRequest
    // ----------------------------------------------------------------

    /**
     * Convertit une demande statistique en réponse avec libellés des enums.
     *
     * @param entity entité StatisticsRequest
     * @return DTO StatisticsRequestResponse
     */
    @Mapping(target = "analysisTypeLabel", expression = "java(analysisTypeLabel(entity.getAnalysisType()))")
    @Mapping(target = "dataFormatLabel",   expression = "java(dataFormatLabel(entity.getDataFormat()))")
    @Mapping(target = "statusLabel",       expression = "java(statisticsStatusLabel(entity.getStatus()))")
    @Mapping(target = "createdAt",         expression = "java(entity.getCreatedAt() != null ? java.time.LocalDateTime.ofInstant(entity.getCreatedAt(), java.time.ZoneOffset.UTC) : null)")
    StatisticsRequestResponse toResponse(StatisticsRequest entity);

    // ----------------------------------------------------------------
    // SponsorCUSLStudy
    // ----------------------------------------------------------------

    /**
     * Convertit une étude sponsor en réponse avec libellé du statut réglementaire.
     *
     * @param entity entité SponsorCUSLStudy
     * @return DTO SponsorStudyResponse
     */
    @Mapping(target = "regulatoryStatusLabel", expression = "java(regulatoryStatusLabel(entity.getRegulatoryStatus()))")
    @Mapping(target = "createdAt",             expression = "java(entity.getCreatedAt() != null ? java.time.LocalDateTime.ofInstant(entity.getCreatedAt(), java.time.ZoneOffset.UTC) : null)")
    SponsorStudyResponse toResponse(SponsorCUSLStudy entity);

    // ----------------------------------------------------------------
    // QualityEvent
    // ----------------------------------------------------------------

    /**
     * Convertit un événement qualité en réponse avec libellés des enums.
     *
     * @param entity entité QualityEvent
     * @return DTO QualityEventResponse
     */
    @Mapping(target = "eventTypeLabel", expression = "java(eventTypeLabel(entity.getEventType()))")
    @Mapping(target = "severityLabel",  expression = "java(severityLabel(entity.getSeverity()))")
    @Mapping(target = "statusLabel",    expression = "java(eventStatusLabel(entity.getStatus()))")
    @Mapping(target = "createdAt",      expression = "java(entity.getCreatedAt() != null ? java.time.LocalDateTime.ofInstant(entity.getCreatedAt(), java.time.ZoneOffset.UTC) : null)")
    QualityEventResponse toResponse(QualityEvent entity);

    // ----------------------------------------------------------------
    // Méthodes par défaut pour les libellés des enums
    // ----------------------------------------------------------------

    /**
     * Retourne le libellé français du type de desk.
     *
     * @param t type de desk ou null
     * @return libellé ou null
     */
    default String deskTypeLabel(DeskType t) {
        return t != null ? t.getLabel() : null;
    }

    /**
     * Retourne le libellé français du type de demande.
     *
     * @param t type de demande ou null
     * @return libellé ou null
     */
    default String requestTypeLabel(RequestType t) {
        return t != null ? t.getLabel() : null;
    }

    /**
     * Retourne le libellé français du statut de demande.
     *
     * @param s statut ou null
     * @return libellé ou null
     */
    default String requestStatusLabel(RequestStatus s) {
        return s != null ? s.getLabel() : null;
    }

    /**
     * Retourne le libellé français de la priorité.
     *
     * @param p priorité ou null
     * @return libellé ou null
     */
    default String priorityLabel(Priority p) {
        return p != null ? p.getLabel() : null;
    }

    /**
     * Retourne le libellé français du type de visite.
     *
     * @param t type de visite ou null
     * @return libellé ou null
     */
    default String visitTypeLabel(VisitType t) {
        return t != null ? t.getLabel() : null;
    }

    /**
     * Retourne le libellé français du statut de visite.
     *
     * @param s statut ou null
     * @return libellé ou null
     */
    default String visitStatusLabel(VisitStatus s) {
        return s != null ? s.getLabel() : null;
    }

    /**
     * Retourne le libellé français du type de contrat.
     *
     * @param t type de contrat ou null
     * @return libellé ou null
     */
    default String contractTypeLabel(ContractType t) {
        return t != null ? t.getLabel() : null;
    }

    /**
     * Retourne le libellé français du statut de contrat.
     *
     * @param s statut ou null
     * @return libellé ou null
     */
    default String contractStatusLabel(ContractStatus s) {
        return s != null ? s.getLabel() : null;
    }

    /**
     * Retourne le libellé français de la devise.
     *
     * @param c devise ou null
     * @return libellé ou null
     */
    default String currencyLabel(Currency c) {
        return c != null ? c.getLabel() : null;
    }

    /**
     * Retourne le libellé français de la périodicité de facturation.
     *
     * @param b périodicité ou null
     * @return libellé ou null
     */
    default String billingScheduleLabel(BillingSchedule b) {
        return b != null ? b.getLabel() : null;
    }

    /**
     * Retourne le libellé français du type d'analyse.
     *
     * @param a type d'analyse ou null
     * @return libellé ou null
     */
    default String analysisTypeLabel(AnalysisType a) {
        return a != null ? a.getLabel() : null;
    }

    /**
     * Retourne le libellé français du format de données.
     *
     * @param d format ou null
     * @return libellé ou null
     */
    default String dataFormatLabel(DataFormat d) {
        return d != null ? d.getLabel() : null;
    }

    /**
     * Retourne le libellé français du statut statistique.
     *
     * @param s statut ou null
     * @return libellé ou null
     */
    default String statisticsStatusLabel(StatisticsStatus s) {
        return s != null ? s.getLabel() : null;
    }

    /**
     * Retourne le libellé français du statut réglementaire.
     *
     * @param r statut ou null
     * @return libellé ou null
     */
    default String regulatoryStatusLabel(RegulatoryStatus r) {
        return r != null ? r.getLabel() : null;
    }

    /**
     * Retourne le libellé français du type d'événement.
     *
     * @param e type d'événement ou null
     * @return libellé ou null
     */
    default String eventTypeLabel(EventType e) {
        return e != null ? e.getLabel() : null;
    }

    /**
     * Retourne le libellé français de la sévérité.
     *
     * @param s sévérité ou null
     * @return libellé ou null
     */
    default String severityLabel(Severity s) {
        return s != null ? s.getLabel() : null;
    }

    /**
     * Retourne le libellé français du statut d'événement.
     *
     * @param s statut ou null
     * @return libellé ou null
     */
    default String eventStatusLabel(EventStatus s) {
        return s != null ? s.getLabel() : null;
    }
}
