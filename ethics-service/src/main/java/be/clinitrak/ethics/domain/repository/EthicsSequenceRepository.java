package be.clinitrak.ethics.domain.repository;

import be.clinitrak.ethics.domain.entity.EthicsSequence;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

/**
 * Repository pour la gestion des séquences de numérotation CE.
 *
 * <p>Utilise un verrou pessimiste (PESSIMISTIC_WRITE) pour garantir l'unicité
 * des numéros générés en cas de soumissions simultanées.
 */
@Repository
public interface EthicsSequenceRepository extends JpaRepository<EthicsSequence, Long> {

    /**
     * Recherche la séquence pour une année et un tenant donnés avec un verrou exclusif.
     *
     * <p>Le verrou PESSIMISTIC_WRITE empêche toute autre transaction de lire
     * ou modifier la ligne tant que la transaction courante n'est pas terminée.
     *
     * @param year     année de la séquence
     * @param tenantId identifiant du tenant
     * @return séquence verrouillée, ou empty si elle n'existe pas encore
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT s FROM EthicsSequence s WHERE s.year = :year AND s.tenantId = :tenantId")
    Optional<EthicsSequence> findByYearAndTenantIdWithLock(
        @Param("year") int year,
        @Param("tenantId") UUID tenantId
    );
}
