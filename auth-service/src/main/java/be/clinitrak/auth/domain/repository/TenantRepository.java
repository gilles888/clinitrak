package be.clinitrak.auth.domain.repository;

import be.clinitrak.auth.domain.entity.Tenant;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

/** Repository JPA pour les tenants. */
@Repository
public interface TenantRepository extends JpaRepository<Tenant, UUID> {

    /**
     * Recherche un tenant par son slug (identifiant court).
     *
     * @param slug identifiant court du tenant
     * @return le tenant actif s'il existe
     */
    Optional<Tenant> findBySlugAndActiveTrue(String slug);

    /**
     * Recherche un tenant par domaine (résolution via sous-domaine).
     *
     * @param domain domaine complet (ex: "saintluc.clinitrak.be")
     * @return le tenant actif s'il existe
     */
    Optional<Tenant> findByDomainAndActiveTrue(String domain);

    /** Vérifie l'unicité du slug. */
    boolean existsBySlug(String slug);
}
