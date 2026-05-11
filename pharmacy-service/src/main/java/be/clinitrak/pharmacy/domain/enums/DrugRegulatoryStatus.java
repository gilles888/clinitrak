package be.clinitrak.pharmacy.domain.enums;

/**
 * Statut réglementaire d'un médicament expérimental.
 *
 * <p>Suit le cycle de vie réglementaire depuis la soumission jusqu'à
 * l'approbation, l'expiration ou le rappel par les autorités.
 */
public enum DrugRegulatoryStatus {

    /** Demande en cours d'examen par les autorités réglementaires. */
    PENDING("En attente"),

    /** Médicament approuvé par les autorités réglementaires. */
    APPROVED("Approuvé"),

    /** Autorisation expirée — renouvellement nécessaire. */
    EXPIRED("Expiré"),

    /** Médicament rappelé par le fabricant ou les autorités. */
    RECALLED("Rappelé");

    private final String label;

    DrugRegulatoryStatus(String label) {
        this.label = label;
    }

    /**
     * Retourne le libellé français du statut réglementaire.
     *
     * @return libellé français
     */
    public String getLabel() {
        return label;
    }
}
