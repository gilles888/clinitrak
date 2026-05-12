package be.clinitrak.exchange.domain.entity;

import be.clinitrak.exchange.domain.enums.ExchangeRequestType;
import be.clinitrak.exchange.domain.enums.ExchangeStatus;
import be.clinitrak.exchange.domain.enums.TargetModule;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

/**
 * Demande d'échange soumise par un utilisateur externe.
 *
 * <p>Une demande représente une interaction formelle entre un acteur externe
 * et un service interne (CE ou CTC). Elle suit un cycle de vie via {@link ExchangeStatus}.
 *
 * <p>Après acceptation, un {@code internalStudyId} est assigné pour lier
 * la demande à une étude dans le study-service.
 */
@Getter
@Setter
@Entity
@Table(name = "exchange_requests")
public class ExchangeRequest extends BaseEntity {

    /** Utilisateur externe à l'origine de cette demande. */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "external_user_id")
    private ExternalUser externalUser;

    /** Module cible de la demande (CE ou CTC). */
    @Enumerated(EnumType.STRING)
    @Column(name = "target_module", nullable = false, length = 50)
    private TargetModule targetModule;

    /** Type de demande (nouvelle étude, amendement, etc.). */
    @Enumerated(EnumType.STRING)
    @Column(name = "request_type", nullable = false, length = 50)
    private ExchangeRequestType requestType;

    /** Titre court de la demande. */
    @Column(name = "title", nullable = false, length = 500)
    private String title;

    /** Description détaillée de la demande. */
    @Column(name = "description", columnDefinition = "TEXT")
    private String description;

    /** Date et heure de soumission formelle de la demande. */
    @Column(name = "submission_date")
    private LocalDateTime submissionDate;

    /** Statut courant de la demande. Vaut {@link ExchangeStatus#DRAFT} à la création. */
    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 50)
    private ExchangeStatus status = ExchangeStatus.DRAFT;

    /** Identifiant de l'étude interne, rempli après acceptation. */
    @Column(name = "internal_study_id", length = 255)
    private String internalStudyId;

    /** Identifiant du tenant auquel appartient cette demande. */
    @Column(name = "tenant_id", nullable = false, length = 255)
    private String tenantId;
}
