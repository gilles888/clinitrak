package be.clinitrak.ctc.domain.enums;

/**
 * Type de visite de monitoring d'une étude clinique.
 *
 * <p>Chaque type correspond à une phase du cycle de vie de l'étude :
 * démarrage, suivi régulier ou clôture.
 */
public enum VisitType {

    /** Visite d'initiation du site avant le début de l'étude. */
    INITIATION("Initiation"),

    /** Visite de suivi routinière en cours d'étude. */
    ROUTINE("Routine"),

    /** Visite de clôture du site en fin d'étude. */
    CLOSE_OUT("Clôture");

    private final String label;

    VisitType(String label) {
        this.label = label;
    }

    /**
     * Retourne le libellé français de ce type de visite.
     *
     * @return libellé lisible en français
     */
    public String getLabel() {
        return label;
    }
}
