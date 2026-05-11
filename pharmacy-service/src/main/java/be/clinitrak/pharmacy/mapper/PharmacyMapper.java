package be.clinitrak.pharmacy.mapper;

import be.clinitrak.pharmacy.domain.entity.Dispensation;
import be.clinitrak.pharmacy.domain.entity.DrugStock;
import be.clinitrak.pharmacy.domain.entity.EmergencyUnblinding;
import be.clinitrak.pharmacy.domain.entity.InvestigationalDrug;
import be.clinitrak.pharmacy.domain.enums.DrugCategory;
import be.clinitrak.pharmacy.domain.enums.DrugForm;
import be.clinitrak.pharmacy.domain.enums.DrugRegulatoryStatus;
import be.clinitrak.pharmacy.domain.enums.StockStatus;
import be.clinitrak.pharmacy.dto.DispensationResponse;
import be.clinitrak.pharmacy.dto.DrugResponse;
import be.clinitrak.pharmacy.dto.EmergencyUnblindingResponse;
import be.clinitrak.pharmacy.dto.StockResponse;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;

/**
 * Mapper MapStruct pour les entités et DTOs du pharmacy-service.
 *
 * <p>Gère les conversions entre entités JPA et records de réponse,
 * ainsi que le mapping des libellés français des enums.
 */
@Mapper(componentModel = "spring")
public interface PharmacyMapper {

    /**
     * Convertit un médicament expérimental en DTO de réponse.
     *
     * @param e entité InvestigationalDrug
     * @return DTO DrugResponse
     */
    @Mapping(target = "formLabel", expression = "java(formLabel(e.getForm()))")
    @Mapping(target = "categoryLabel", expression = "java(categoryLabel(e.getCategory()))")
    @Mapping(target = "regulatoryStatusLabel", expression = "java(regulatoryStatusLabel(e.getRegulatoryStatus()))")
    @Mapping(target = "createdAt", expression = "java(toLocalDateTime(e.getCreatedAt()))")
    DrugResponse toResponse(InvestigationalDrug e);

    /**
     * Convertit un stock de médicament en DTO de réponse.
     *
     * @param e entité DrugStock
     * @return DTO StockResponse
     */
    @Mapping(target = "drugId", expression = "java(e.getDrug() != null ? e.getDrug().getId() : null)")
    @Mapping(target = "drugName", expression = "java(e.getDrug() != null ? e.getDrug().getDrugName() : null)")
    @Mapping(target = "statusLabel", expression = "java(statusLabel(e.getStatus()))")
    @Mapping(target = "createdAt", expression = "java(toLocalDateTime(e.getCreatedAt()))")
    StockResponse toResponse(DrugStock e);

    /**
     * Convertit une dispensation en DTO de réponse.
     *
     * @param e entité Dispensation
     * @return DTO DispensationResponse
     */
    @Mapping(target = "drugId", expression = "java(e.getDrug() != null ? e.getDrug().getId() : null)")
    @Mapping(target = "drugName", expression = "java(e.getDrug() != null ? e.getDrug().getDrugName() : null)")
    @Mapping(target = "createdAt", expression = "java(toLocalDateTime(e.getCreatedAt()))")
    DispensationResponse toResponse(Dispensation e);

    /**
     * Convertit une levée d'insu en DTO de réponse.
     *
     * @param e entité EmergencyUnblinding
     * @return DTO EmergencyUnblindingResponse
     */
    @Mapping(target = "createdAt", expression = "java(toLocalDateTime(e.getCreatedAt()))")
    EmergencyUnblindingResponse toResponse(EmergencyUnblinding e);

    /**
     * Retourne le libellé français de la forme pharmaceutique.
     *
     * @param f forme pharmaceutique
     * @return libellé ou null
     */
    default String formLabel(DrugForm f) {
        return f != null ? f.getLabel() : null;
    }

    /**
     * Retourne le libellé français de la catégorie de médicament.
     *
     * @param c catégorie
     * @return libellé ou null
     */
    default String categoryLabel(DrugCategory c) {
        return c != null ? c.getLabel() : null;
    }

    /**
     * Retourne le libellé français du statut réglementaire.
     *
     * @param r statut réglementaire
     * @return libellé ou null
     */
    default String regulatoryStatusLabel(DrugRegulatoryStatus r) {
        return r != null ? r.getLabel() : null;
    }

    /**
     * Retourne le libellé français du statut de stock.
     *
     * @param s statut de stock
     * @return libellé ou null
     */
    default String statusLabel(StockStatus s) {
        return s != null ? s.getLabel() : null;
    }

    /**
     * Convertit un {@link Instant} en {@link LocalDateTime} dans le fuseau local.
     *
     * @param instant instant UTC
     * @return LocalDateTime local ou null
     */
    default LocalDateTime toLocalDateTime(Instant instant) {
        return instant != null ? LocalDateTime.ofInstant(instant, ZoneId.systemDefault()) : null;
    }
}
