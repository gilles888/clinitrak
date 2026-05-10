package be.clinitrak.auth.security;

import be.clinitrak.auth.domain.entity.User;
import be.clinitrak.auth.domain.repository.TenantRepository;
import be.clinitrak.auth.domain.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

/**
 * Implémentation de {@link UserDetailsService} pour Spring Security.
 *
 * <p>Le format du username attendu est {@code email:tenantId} pour permettre
 * la résolution multi-tenant lors de la validation JWT.
 */
@Service
@RequiredArgsConstructor
public class UserDetailsServiceImpl implements UserDetailsService {

    private final UserRepository userRepository;
    private final TenantRepository tenantRepository;

    /**
     * Charge un utilisateur par son email et son tenant.
     *
     * @param username format attendu : {@code email:tenantId}
     * @return détails Spring Security de l'utilisateur
     * @throws UsernameNotFoundException si l'utilisateur ou le tenant n'existe pas
     */
    @Override
    @Transactional(readOnly = true)
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        // Format interne : "email:tenantId"
        String[] parts = username.split(":", 2);
        if (parts.length != 2) {
            throw new UsernameNotFoundException("Format username invalide : " + username);
        }

        String email = parts[0];
        UUID tenantId;
        try {
            tenantId = UUID.fromString(parts[1]);
        } catch (IllegalArgumentException e) {
            throw new UsernameNotFoundException("TenantId invalide : " + parts[1]);
        }

        User user = userRepository.findByEmailAndTenantId(email, tenantId)
            .orElseThrow(() -> new UsernameNotFoundException(
                "Utilisateur non trouvé : " + email + " (tenant: " + tenantId + ")"
            ));

        List<SimpleGrantedAuthority> authorities = user.getRoles().stream()
            .flatMap(role -> {
                // L'autorité du rôle lui-même
                var roleAuthority = new SimpleGrantedAuthority(role.getName());
                // Les permissions associées au rôle
                var permAuthorities = role.getPermissions().stream()
                    .map(perm -> new SimpleGrantedAuthority(perm.getName()));
                return java.util.stream.Stream.concat(java.util.stream.Stream.of(roleAuthority), permAuthorities);
            })
            .distinct()
            .toList();

        return org.springframework.security.core.userdetails.User.builder()
            .username(email + ":" + tenantId)
            .password(user.getPasswordHash())
            .authorities(authorities)
            .accountLocked(user.isAccountLocked())
            .disabled(!user.isEnabled())
            .build();
    }
}
