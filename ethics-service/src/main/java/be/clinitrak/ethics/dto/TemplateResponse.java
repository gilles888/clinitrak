package be.clinitrak.ethics.dto;

import be.clinitrak.ethics.domain.enums.TemplateType;

import java.util.UUID;

/**
 * Réponse représentant un modèle de correspondance (sans le contenu HTML pour les listes).
 *
 * <p>Le champ {@code content} est volontairement absent pour alléger les réponses de liste.
 * Utiliser l'endpoint détail pour obtenir le contenu complet.
 *
 * @param id           identifiant UUID du template
 * @param templateCode code unique du template (ex: "APPROVAL_FR")
 * @param templateName nom descriptif du template
 * @param templateType type fonctionnel du template
 * @param language     code langue (ex: "fr", "nl")
 * @param isActive     indique si le template est actif
 * @param subject      sujet de l'email généré
 */
public record TemplateResponse(
    UUID id,
    String templateCode,
    String templateName,
    TemplateType templateType,
    String language,
    boolean isActive,
    String subject
) {}
