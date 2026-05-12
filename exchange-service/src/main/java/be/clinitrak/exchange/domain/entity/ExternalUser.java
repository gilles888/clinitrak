package be.clinitrak.exchange.domain.entity;

import be.clinitrak.exchange.domain.enums.ExternalUserRole;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

/**
 * Utilisateur externe enregistré dans le portail d'échange CliniTrak.
 *
 * <p>Représente un acteur externe (firme, investigateur, demandeur CE) qui accède
 * au portail via son propre JWT distinct du JWT interne.
 *
 * <p>L'email doit être vérifié avant la première connexion via le token
 * {@code emailVerificationToken}.
 */
@Getter
@Setter
@Entity
@Table(name = "exchange_external_users")
public class ExternalUser extends BaseEntity {

    /** Adresse email unique servant d'identifiant de connexion. */
    @Column(name = "email", nullable = false, unique = true, length = 255)
    private String email;

    /** Prénom de l'utilisateur externe. */
    @Column(name = "first_name", nullable = false, length = 100)
    private String firstName;

    /** Nom de famille de l'utilisateur externe. */
    @Column(name = "last_name", nullable = false, length = 100)
    private String lastName;

    /** Organisation ou institution de l'utilisateur (facultatif). */
    @Column(name = "organization", length = 255)
    private String organization;

    /** Rôle de l'utilisateur externe dans le système. */
    @Enumerated(EnumType.STRING)
    @Column(name = "role", nullable = false, length = 50)
    private ExternalUserRole role;

    /** Indique si l'email a été vérifié. Vaut {@code false} à la création. */
    @Column(name = "verified_email", nullable = false)
    private boolean verifiedEmail = false;

    /** Token UUID envoyé par email pour la vérification du compte. */
    @Column(name = "email_verification_token", length = 255)
    private String emailVerificationToken;

    /** Mot de passe haché en BCrypt (force 12). */
    @Column(name = "password_hash", nullable = false, length = 255)
    private String passwordHash;

    /** Date et heure de création du compte (colonne locale, distinct de BaseEntity.createdAt). */
    @Column(name = "created_at_local")
    private LocalDateTime createdAtLocal;
}
