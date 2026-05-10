package be.clinitrak.ethics.domain.entity;

import be.clinitrak.ethics.domain.enums.TemplateType;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.util.UUID;

/**
 * Modèle de correspondance du Comité d'Éthique stocké en base de données.
 *
 * <p>Les templates utilisent la syntaxe Thymeleaf avec des variables de substitution
 * de la forme {@code [[${variableName}]]} compatibles avec le mode HTML strict.
 *
 * <p>Les templates globaux (tenant_id = null) sont disponibles pour tous les tenants.
 * Un tenant peut surcharger un template global en créant son propre template
 * avec le même {@code templateCode}.
 */
@Getter
@Setter
@Entity
@Table(
    name = "correspondence_templates",
    indexes = {
        @Index(name = "idx_ct_template_type", columnList = "template_type"),
        @Index(name = "idx_ct_is_active", columnList = "is_active")
    }
)
public class CorrespondenceTemplate extends BaseEntity {

    /**
     * Identifiant du tenant propriétaire (null pour les templates globaux
     * disponibles pour tous les tenants).
     */
    @Column(name = "tenant_id")
    private UUID tenantId;

    /** Code unique du template (ex: "APPROVAL_FR", "REJECTION_FR"). */
    @Column(name = "template_code", nullable = false, unique = true, length = 50)
    private String templateCode;

    /** Nom descriptif du template. */
    @Column(name = "template_name", nullable = false, length = 255)
    private String templateName;

    /** Type fonctionnel du template. */
    @Enumerated(EnumType.STRING)
    @Column(name = "template_type", nullable = false, length = 50)
    private TemplateType templateType;

    /**
     * Contenu HTML Thymeleaf du template.
     * Utilise la syntaxe {@code [[${variable}]]} pour les substitutions inline.
     */
    @Column(name = "content", columnDefinition = "TEXT")
    private String content;

    /** Code de langue du template (ex: "fr", "nl", "en"). */
    @Column(name = "language", length = 5)
    private String language = "fr";

    /** Indique si ce template est actif et utilisable. */
    @Column(name = "is_active", nullable = false)
    private boolean isActive = true;

    /** Sujet de l'email généré depuis ce template. */
    @Column(name = "subject", length = 255)
    private String subject;
}
