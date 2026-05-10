package be.clinitrak.ctc.domain.enums;

/**
 * Type de demande soumise au desk CTC.
 *
 * <p>Couvre le cycle de vie d'une étude : de la soumission initiale
 * à la clôture, en passant par les amendements et extensions.
 */
public enum RequestType {

    /** Nouvelle étude soumise pour la première fois. */
    NEW_STUDY("Nouvelle étude"),

    /** Amendement à une étude existante. */
    AMENDMENT("Amendement"),

    /** Demande d'extension du périmètre ou de la durée. */
    EXTENSION("Extension"),

    /** Demande de clôture d'une étude. */
    CLOSURE("Clôture");

    private final String label;

    RequestType(String label) {
        this.label = label;
    }

    /**
     * Retourne le libellé français de ce type de demande.
     *
     * @return libellé lisible en français
     */
    public String getLabel() {
        return label;
    }
}
