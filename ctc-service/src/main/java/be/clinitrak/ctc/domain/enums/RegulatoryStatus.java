package be.clinitrak.ctc.domain.enums;

/**
 * Statut réglementaire d'une étude gérée par le sponsor CTC.
 *
 * <p>Reflète l'avancement des démarches réglementaires auprès
 * des autorités compétentes (FAMHP, CE, etc.).
 */
public enum RegulatoryStatus {

    /** Dossier réglementaire en cours de préparation. */
    IN_PREPARATION("En préparation"),

    /** Dossier soumis aux autorités réglementaires. */
    SUBMITTED("Soumis"),

    /** Autorisation obtenue, étude approuvée. */
    APPROVED("Approuvé"),

    /** Étude en cours avec statut réglementaire actif. */
    ONGOING("En cours"),

    /** Démarches réglementaires terminées (fin d'étude). */
    COMPLETED("Terminé");

    private final String label;

    RegulatoryStatus(String label) {
        this.label = label;
    }

    /**
     * Retourne le libellé français de ce statut réglementaire.
     *
     * @return libellé lisible en français
     */
    public String getLabel() {
        return label;
    }
}
