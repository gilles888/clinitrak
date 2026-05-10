package be.clinitrak.study.domain.entity;

import be.clinitrak.study.domain.enums.ContactType;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.util.UUID;

/**
 * Contact associé à une étude clinique (investigateur, CRA, coordinateur, promoteur).
 *
 * <p>Une étude peut avoir plusieurs contacts de différents types.
 * Le contact principal ({@code isPrimary = true}) est le point de contact privilégié.
 */
@Getter
@Setter
@Entity
@Table(
    name = "study_contacts",
    indexes = {
        @Index(name = "idx_contact_study", columnList = "study_id"),
        @Index(name = "idx_contact_tenant", columnList = "tenant_id")
    }
)
public class StudyContact extends BaseEntity {

    /**
     * Étude clinique à laquelle appartient ce contact.
     * Chargé en mode LAZY pour éviter les jointures inutiles.
     */
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "study_id", nullable = false)
    private ClinicalStudy study;

    /**
     * Identifiant du tenant pour l'isolation des données.
     */
    @Column(name = "tenant_id", nullable = false)
    private UUID tenantId;

    /**
     * Type de contact (investigateur, CRA, coordinateur, promoteur).
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "contact_type", nullable = false, length = 30)
    private ContactType contactType;

    /**
     * Prénom du contact.
     */
    @Column(name = "first_name", nullable = false, length = 100)
    private String firstName;

    /**
     * Nom de famille du contact.
     */
    @Column(name = "last_name", nullable = false, length = 100)
    private String lastName;

    /**
     * Adresse email du contact.
     */
    @Column(name = "email", length = 255)
    private String email;

    /**
     * Numéro de téléphone du contact.
     */
    @Column(name = "phone", length = 50)
    private String phone;

    /**
     * Organisation ou institution d'appartenance du contact.
     */
    @Column(name = "organization", length = 255)
    private String organization;

    /**
     * Indique si ce contact est le contact principal pour ce type.
     */
    @Column(name = "is_primary", nullable = false)
    private boolean isPrimary = false;

    /**
     * Indique si ce contact est actif.
     * Un contact inactif n'apparaît pas dans les listes standard.
     */
    @Column(name = "active", nullable = false)
    private boolean active = true;
}
