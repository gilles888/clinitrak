package be.clinitrak.auth.domain.repository;

import be.clinitrak.auth.domain.entity.Role;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/** Repository JPA pour les rôles. */
@Repository
public interface RoleRepository extends JpaRepository<Role, UUID> {

    /**
     * Recherche un rôle par son nom dans un tenant donné ou dans les rôles globaux.
     *
     * @param name     nom du rôle
     * @param tenantId tenant concerné (null pour les rôles globaux)
     * @return le rôle s'il existe
     */
    Optional<Role> findByNameAndTenantId(String name, UUID tenantId);

    /** Rôles globaux (tenantId = null). */
    List<Role> findByTenantIdIsNull();

    /** Tous les rôles d'un tenant (inclut les rôles globaux si tenantId est null). */
    List<Role> findByTenantId(UUID tenantId);
}
