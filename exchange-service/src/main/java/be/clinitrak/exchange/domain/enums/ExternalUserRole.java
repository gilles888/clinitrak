package be.clinitrak.exchange.domain.enums;

/**
 * Rôle d'un utilisateur externe dans le système d'échange CliniTrak.
 *
 * <p>Détermine les permissions et le type de demandes qu'un utilisateur
 * externe peut soumettre.
 */
public enum ExternalUserRole {

    /** Représentant d'une firme pharmaceutique ou dispositif médical. */
    COMPANY("Firme"),

    /** Médecin investigateur principal ou co-investigateur. */
    INVESTIGATOR("Investigateur"),

    /** Personne autorisée à soumettre des demandes au Comité d'Éthique. */
    CE_REQUESTOR("Demandeur CE");

    private final String label;

    ExternalUserRole(String label) {
        this.label = label;
    }

    /**
     * Retourne le libellé français du rôle.
     *
     * @return libellé du rôle
     */
    public String getLabel() {
        return label;
    }
}
