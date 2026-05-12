package be.clinitrak.exchange.domain.repository;

import be.clinitrak.exchange.domain.entity.ExternalUser;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

/**
 * Repository Spring Data JPA pour les utilisateurs externes.
 */
@Repository
public interface ExternalUserRepository extends JpaRepository<ExternalUser, UUID> {

    /**
     * Recherche un utilisateur externe par email (insensible à la casse).
     *
     * @param email adresse email
     * @return utilisateur externe ou empty
     */
    Optional<ExternalUser> findByEmailIgnoreCaseAndDeletedFalse(String email);

    /**
     * Vérifie si un email est déjà utilisé.
     *
     * @param email adresse email
     * @return true si l'email existe déjà
     */
    boolean existsByEmailIgnoreCaseAndDeletedFalse(String email);

    /**
     * Recherche un utilisateur par son token de vérification d'email.
     *
     * @param token token de vérification
     * @return utilisateur externe ou empty
     */
    Optional<ExternalUser> findByEmailVerificationTokenAndDeletedFalse(String token);
}
