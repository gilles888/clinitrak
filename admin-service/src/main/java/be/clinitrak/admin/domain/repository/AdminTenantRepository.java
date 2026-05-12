package be.clinitrak.admin.domain.repository;

import be.clinitrak.admin.domain.entity.AdminTenant;
import be.clinitrak.admin.domain.enums.TenantStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Repository Spring Data JPA pour les tenants administratifs.
 */
@Repository
public interface AdminTenantRepository extends JpaRepository<AdminTenant, UUID> {

    /**
     * Vérifie si un slug est déjà utilisé par un tenant actif.
     *
     * @param slug slug à vérifier
     * @return true si le slug existe déjà
     */
    boolean existsBySlugAndDeletedFalse(String slug);

    /**
     * Retourne tous les tenants non supprimés.
     *
     * @return liste des tenants actifs ou inactifs
     */
    List<AdminTenant> findAllByDeletedFalse();

    /**
     * Recherche un tenant par son slug.
     *
     * @param slug slug du tenant
     * @return tenant ou empty
     */
    Optional<AdminTenant> findBySlugAndDeletedFalse(String slug);

    /**
     * Retourne les tenants par statut.
     *
     * @param status statut à filtrer
     * @return liste des tenants au statut donné
     */
    List<AdminTenant> findByStatusAndDeletedFalse(TenantStatus status);
}
