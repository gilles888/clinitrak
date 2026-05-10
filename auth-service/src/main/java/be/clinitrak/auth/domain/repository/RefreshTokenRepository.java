package be.clinitrak.auth.domain.repository;

import be.clinitrak.auth.domain.entity.RefreshToken;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

/** Repository JPA pour les refresh tokens. */
@Repository
public interface RefreshTokenRepository extends JpaRepository<RefreshToken, UUID> {

    /**
     * Recherche un token par sa valeur (utilisé lors du rafraîchissement).
     *
     * @param token valeur du refresh token
     * @return le token s'il existe
     */
    Optional<RefreshToken> findByToken(String token);

    /**
     * Révoque tous les refresh tokens actifs d'un utilisateur (logout de tous les appareils).
     *
     * @param userId identifiant de l'utilisateur
     */
    @Modifying
    @Query("UPDATE RefreshToken rt SET rt.revoked = true WHERE rt.user.id = :userId AND rt.revoked = false")
    void revokeAllByUserId(@Param("userId") UUID userId);

    /**
     * Supprime les tokens expirés pour la maintenance périodique.
     *
     * @param cutoff timestamp avant lequel les tokens sont supprimés
     * @return nombre de tokens supprimés
     */
    @Modifying
    @Query("DELETE FROM RefreshToken rt WHERE rt.expiresAt < :cutoff OR rt.revoked = true")
    int deleteExpiredAndRevoked(@Param("cutoff") Instant cutoff);
}
