package be.clinitrak.document.domain.enums;

/**
 * Module fonctionnel source d'un document dans la plateforme CliniTrak.
 * Permet de classer et filtrer les documents par domaine métier.
 */
public enum ModuleSource {

    /** Documents issus du module de gestion des études cliniques. */
    STUDY,

    /** Documents issus du module Comité d'Éthique. */
    ETHICS,

    /** Documents issus du module Centre de Thérapie Cellulaire. */
    CTC,

    /** Documents issus du module Pharmacie. */
    PHARMACY,

    /** Documents issus du portail d'échanges externes. */
    EXCHANGE
}
