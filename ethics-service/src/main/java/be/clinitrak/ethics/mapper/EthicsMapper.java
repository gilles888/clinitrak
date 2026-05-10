package be.clinitrak.ethics.mapper;

import be.clinitrak.ethics.domain.entity.*;
import be.clinitrak.ethics.domain.enums.*;
import be.clinitrak.ethics.dto.*;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

/**
 * Mapper MapStruct pour la conversion entre entités JPA et DTOs du ethics-service.
 *
 * <p>MapStruct génère l'implémentation à la compilation.
 * Le composant est enregistré comme bean Spring via {@code componentModel = "spring"}.
 */
@Mapper(componentModel = "spring")
public interface EthicsMapper {

    /**
     * Convertit un avis CE en réponse complète avec libellés des enums.
     *
     * @param review entité EthicsReview
     * @return DTO EthicsReviewResponse avec tous les champs
     */
    @Mapping(target = "reviewTypeLabel",  expression = "java(mapReviewTypeLabel(review.getReviewType()))")
    @Mapping(target = "decisionLabel",    expression = "java(mapDecisionLabel(review.getDecision()))")
    EthicsReviewResponse toResponse(EthicsReview review);

    /**
     * Crée une entité EthicsReview depuis une requête de création.
     * Les champs gérés par le service (ethicsNumber, tenantId, decision, timestamps) sont ignorés.
     *
     * @param request DTO de création
     * @return entité EthicsReview non persistée
     */
    @Mapping(target = "id",             ignore = true)
    @Mapping(target = "createdAt",      ignore = true)
    @Mapping(target = "updatedAt",      ignore = true)
    @Mapping(target = "createdBy",      ignore = true)
    @Mapping(target = "updatedBy",      ignore = true)
    @Mapping(target = "deleted",        ignore = true)
    @Mapping(target = "version",        ignore = true)
    @Mapping(target = "tenantId",       ignore = true)
    @Mapping(target = "ethicsNumber",   ignore = true)
    @Mapping(target = "decision",       ignore = true)
    @Mapping(target = "decisionDate",   ignore = true)
    @Mapping(target = "reviewDate",     ignore = true)
    @Mapping(target = "nextReviewDate", ignore = true)
    @Mapping(target = "reminderSent",   ignore = true)
    EthicsReview fromCreateRequest(EthicsReviewCreateRequest request);

    /**
     * Convertit une réunion en réponse avec libellés et nombre d'items.
     *
     * <p>Le champ {@code agendaItemCount} est calculé dans le service et non mappé ici.
     *
     * @param meeting entité Meeting
     * @return DTO MeetingResponse
     */
    @Mapping(target = "meetingTypeLabel", expression = "java(mapMeetingTypeLabel(meeting.getMeetingType()))")
    @Mapping(target = "statusLabel",      expression = "java(mapMeetingStatusLabel(meeting.getStatus()))")
    @Mapping(target = "agendaItemCount",  constant = "0")
    MeetingResponse toMeetingResponse(Meeting meeting);

    /**
     * Convertit un item d'ordre du jour en réponse avec libellé de décision.
     *
     * @param item entité MeetingAgendaItem
     * @return DTO AgendaItemResponse
     */
    @Mapping(target = "decisionLabel", expression = "java(mapDecisionLabel(item.getDecision()))")
    AgendaItemResponse toAgendaItemResponse(MeetingAgendaItem item);

    /**
     * Convertit un rapport annuel en réponse.
     *
     * <p>Le champ {@code daysUntilDue} est calculé dans le service.
     *
     * @param report entité AnnualReport
     * @return DTO AnnualReportResponse
     */
    @Mapping(target = "statusLabel",  expression = "java(mapAnnualReportStatusLabel(report.getStatus()))")
    @Mapping(target = "daysUntilDue", constant = "0L")
    AnnualReportResponse toAnnualReportResponse(AnnualReport report);

    /**
     * Convertit une correspondance en réponse.
     *
     * @param corr entité Correspondence
     * @return DTO CorrespondenceResponse
     */
    CorrespondenceResponse toCorrespondenceResponse(Correspondence corr);

    /**
     * Convertit un template de correspondance en réponse (sans le contenu HTML).
     *
     * @param template entité CorrespondenceTemplate
     * @return DTO TemplateResponse
     */
    TemplateResponse toTemplateResponse(CorrespondenceTemplate template);

    // ----------------------------------------------------------------
    // Méthodes par défaut pour mapper les libellés des enums
    // ----------------------------------------------------------------

    /**
     * Retourne le libellé français du type d'avis.
     *
     * @param t type d'avis ou null
     * @return libellé ou null
     */
    default String mapReviewTypeLabel(ReviewType t) {
        return t != null ? t.getLabel() : null;
    }

    /**
     * Retourne le libellé français de la décision CE.
     *
     * @param d décision ou null
     * @return libellé ou null
     */
    default String mapDecisionLabel(ReviewDecision d) {
        return d != null ? d.getLabel() : null;
    }

    /**
     * Retourne le libellé français du type de réunion.
     *
     * @param t type de réunion ou null
     * @return libellé ou null
     */
    default String mapMeetingTypeLabel(MeetingType t) {
        return t != null ? t.getLabel() : null;
    }

    /**
     * Retourne le libellé français du statut de réunion.
     *
     * @param s statut ou null
     * @return libellé ou null
     */
    default String mapMeetingStatusLabel(MeetingStatus s) {
        return s != null ? s.getLabel() : null;
    }

    /**
     * Retourne le libellé français du statut de rapport annuel.
     *
     * @param s statut ou null
     * @return libellé ou null
     */
    default String mapAnnualReportStatusLabel(AnnualReportStatus s) {
        return s != null ? s.getLabel() : null;
    }
}
