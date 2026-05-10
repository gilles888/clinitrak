package be.clinitrak.ctc.domain.enums;

/**
 * Type d'événement qualité survenu dans le cadre d'une étude clinique.
 *
 * <p>Couvre les déviations protocolaires, les événements indésirables graves,
 * les actions correctives et les audits.
 */
public enum EventType {

    /** Déviation au protocole de l'étude. */
    DEVIATION("Déviation"),

    /** Événement Indésirable Grave (SAE - Serious Adverse Event). */
    SAE("EIG"),

    /** Action Corrective et Préventive. */
    CAPA("CAPA"),

    /** Audit interne ou externe du processus. */
    AUDIT("Audit");

    private final String label;

    EventType(String label) {
        this.label = label;
    }

    /**
     * Retourne le libellé français de ce type d'événement.
     *
     * @return libellé lisible en français
     */
    public String getLabel() {
        return label;
    }
}
