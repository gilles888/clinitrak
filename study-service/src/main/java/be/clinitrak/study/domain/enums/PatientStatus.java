package be.clinitrak.study.domain.enums;

import com.fasterxml.jackson.annotation.JsonValue;

/**
 * Statut d'un patient dans une étude clinique.
 *
 * <p>Conforme au CDISC CDASH standard pour le suivi des participants.
 */
public enum PatientStatus {

    /** Patient en cours de pré-sélection (screening). */
    SCREENED("Pré-sélectionné"),

    /** Patient inclus dans l'étude (randomisé ou inscrit). */
    ENROLLED("Inclus"),

    /** Patient en cours de traitement/suivi. */
    ONGOING("En cours"),

    /** Patient ayant complété l'étude conformément au protocole. */
    COMPLETED("Complété"),

    /** Patient retiré de l'étude (volontairement ou sur décision médicale). */
    WITHDRAWN("Retiré"),

    /** Patient ayant échoué au screening (critères non remplis). */
    SCREEN_FAILED("Échec de sélection");

    private final String label;

    PatientStatus(String label) {
        this.label = label;
    }

    /**
     * Retourne le libellé français du statut patient.
     *
     * @return libellé en français
     */
    public String getLabel() {
        return label;
    }

    /**
     * Retourne le nom de l'enum pour la sérialisation JSON.
     *
     * @return nom de la constante enum
     */
    @JsonValue
    public String getName() {
        return name();
    }
}
