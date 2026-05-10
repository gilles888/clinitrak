package be.clinitrak.auth.domain.repository;

import be.clinitrak.auth.domain.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

/**
 * Repository JPA pour les utilisateurs.
 *
 * <p>Toutes les requêtes filtrent implicitement sur {@code deleted = false}.
 */
@Repository
public interface UserRepository extends JpaRepository<User, UUID> {

    /**
     * Recherche un utilisateur par email et tenant (login).
     *
     * @param email    adresse email (insensible à la casse)
     * @param tenantId identifiant du tenant
     * @return l'utilisateur s'il existe et n'est pas supprimé
     */
    @Query("SELECT u FROM User u WHERE LOWER(u.email) = LOWER(:email) AND u.tenantId = :tenantId AND u.deleted = false")
    Optional<User> findByEmailAndTenantId(@Param("email") String email, @Param("tenantId") UUID tenantId);

    /**
     * Vérifie l'unicité de l'email dans un tenant.
     *
     * @param email    adresse email
     * @param tenantId identifiant du tenant
     * @return true si l'email est déjà utilisé
     */
    boolean existsByEmailIgnoreCaseAndTenantId(String email, UUID tenantId);
}
