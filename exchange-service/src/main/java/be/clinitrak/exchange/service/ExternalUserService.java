package be.clinitrak.exchange.service;

import be.clinitrak.exchange.domain.entity.ExternalUser;
import be.clinitrak.exchange.domain.repository.ExternalUserRepository;
import be.clinitrak.exchange.dto.*;
import be.clinitrak.exchange.exception.ExchangeException;
import be.clinitrak.exchange.exception.ExchangeNotFoundException;
import be.clinitrak.exchange.security.ExchangeJwtService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Service métier pour la gestion des utilisateurs externes du portail d'échange.
 *
 * <p>Gère l'inscription, la vérification d'email et la connexion des utilisateurs
 * externes (firmes, investigateurs, demandeurs CE).
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ExternalUserService {

    private final ExternalUserRepository externalUserRepository;
    private final PasswordEncoder passwordEncoder;
    private final ExchangeJwtService exchangeJwtService;

    /**
     * Enregistre un nouvel utilisateur externe.
     *
     * <p>Vérifie l'unicité de l'email, hache le mot de passe en BCrypt,
     * génère un token de vérification d'email et sauvegarde l'utilisateur.
     * L'envoi de l'email de vérification est délégué au notification-service.
     *
     * @param req données d'enregistrement
     * @return représentation de l'utilisateur créé
     * @throws ExchangeException si l'email est déjà utilisé
     */
    @Transactional
    public ExternalUserResponse register(ExternalUserRegisterRequest req) {
        if (externalUserRepository.existsByEmailIgnoreCaseAndDeletedFalse(req.email())) {
            throw new ExchangeException("Un compte existe déjà avec l'email : " + req.email());
        }

        String verificationToken = UUID.randomUUID().toString();
        String hashedPassword = passwordEncoder.encode(req.password());

        ExternalUser user = new ExternalUser();
        user.setEmail(req.email().toLowerCase().trim());
        user.setFirstName(req.firstName());
        user.setLastName(req.lastName());
        user.setOrganization(req.organization());
        user.setRole(req.role());
        user.setPasswordHash(hashedPassword);
        user.setVerifiedEmail(false);
        user.setEmailVerificationToken(verificationToken);
        user.setCreatedAtLocal(LocalDateTime.now());

        ExternalUser saved = externalUserRepository.save(user);

        // Email de vérification délégué au notification-service
        log.info("Email verification token for {}: {}", saved.getEmail(), verificationToken);

        return toResponse(saved);
    }

    /**
     * Connecte un utilisateur externe et génère un JWT externe.
     *
     * @param req données de connexion
     * @return token JWT externe et informations de l'utilisateur
     * @throws ExchangeNotFoundException si l'utilisateur n'existe pas
     * @throws ExchangeException         si le mot de passe est incorrect ou l'email non vérifié
     */
    @Transactional(readOnly = true)
    public ExternalUserLoginResponse login(ExternalUserLoginRequest req) {
        ExternalUser user = externalUserRepository
            .findByEmailIgnoreCaseAndDeletedFalse(req.email())
            .orElseThrow(() -> new ExchangeNotFoundException("Utilisateur externe introuvable : " + req.email()));

        if (!passwordEncoder.matches(req.password(), user.getPasswordHash())) {
            throw new ExchangeException("Mot de passe incorrect");
        }

        if (!user.isVerifiedEmail()) {
            throw new ExchangeException("Email non vérifié. Veuillez vérifier votre boîte email.");
        }

        String token = exchangeJwtService.generateToken(
            user.getId(),
            user.getEmail(),
            user.getRole().name()
        );

        return new ExternalUserLoginResponse(token, "Bearer", toResponse(user));
    }

    /**
     * Vérifie l'email d'un utilisateur externe via son token de vérification.
     *
     * @param token token de vérification envoyé par email
     * @return représentation de l'utilisateur mis à jour
     * @throws ExchangeNotFoundException si le token est invalide ou déjà utilisé
     */
    @Transactional
    public ExternalUserResponse verifyEmail(String token) {
        ExternalUser user = externalUserRepository
            .findByEmailVerificationTokenAndDeletedFalse(token)
            .orElseThrow(() -> new ExchangeNotFoundException(
                "Token de vérification invalide ou déjà utilisé : " + token));

        user.setVerifiedEmail(true);
        user.setEmailVerificationToken(null);
        ExternalUser saved = externalUserRepository.save(user);

        log.info("Email vérifié avec succès pour : {}", saved.getEmail());
        return toResponse(saved);
    }

    /**
     * Convertit une entité {@link ExternalUser} en DTO de réponse.
     *
     * @param user entité utilisateur externe
     * @return DTO de réponse
     */
    private ExternalUserResponse toResponse(ExternalUser user) {
        return new ExternalUserResponse(
            user.getId(),
            user.getEmail(),
            user.getFirstName(),
            user.getLastName(),
            user.getOrganization(),
            user.getRole(),
            user.getRole().getLabel(),
            user.isVerifiedEmail(),
            user.getCreatedAt() != null
                ? java.time.LocalDateTime.ofInstant(user.getCreatedAt(), java.time.ZoneId.systemDefault())
                : null
        );
    }
}
