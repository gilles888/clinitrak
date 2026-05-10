package be.clinitrak.ethics.domain.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.util.UUID;

/**
 * Entité de séquence pour la génération des numéros CE par année et par tenant.
 *
 * <p>Chaque combinaison (année, tenantId) dispose de son propre compteur.
 * Un verrou pessimiste est utilisé lors de la lecture pour éviter les doublons
 * en cas de soumissions simultanées.
 *
 * <p>Cette entité n'étend pas {@link BaseEntity} car elle ne nécessite pas d'audit.
 */
@Getter
@Setter
@Entity
@Table(
    name = "ethics_sequences",
    uniqueConstraints = @UniqueConstraint(
        name = "uk_seq_year_tenant",
        columnNames = {"year", "tenant_id"}
    )
)
public class EthicsSequence {

    /** Identifiant technique auto-incrémenté. */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** Année de la séquence (ex: 2026). */
    @Column(name = "year", nullable = false)
    private int year;

    /** Identifiant du tenant auquel appartient cette séquence. */
    @Column(name = "tenant_id", nullable = false)
    private UUID tenantId;

    /** Dernière valeur utilisée de la séquence (démarre à 0). */
    @Column(name = "last_value", nullable = false)
    private long lastValue = 0L;

    /** Version pour l'optimistic locking. */
    @Version
    @Column(name = "version")
    private Long version;
}
