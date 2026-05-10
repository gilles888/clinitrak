package be.clinitrak.auth.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.*;

/**
 * DTO immuable (record) pour l'enregistrement d'un nouvel utilisateur.
 *
 * @param email       adresse email unique dans le tenant
 * @param password    mot de passe (min 8 car., complexité vérifiée côté service)
 * @param firstName   prénom
 * @param lastName    nom de famille
 * @param roleName    rôle demandé (ex: "ROLE_INVESTIGATOR"), validé par un admin
 */
@Schema(description = "Requête d'enregistrement d'un nouvel utilisateur")
public record RegisterRequest(

    @Schema(description = "Email unique dans le tenant", example = "jean.martin@saintluc.be")
    @NotBlank(message = "L'email est obligatoire")
    @Email(message = "Format d'email invalide")
    @Size(max = 255)
    String email,

    @Schema(description = "Mot de passe (min. 8 caractères)")
    @NotBlank(message = "Le mot de passe est obligatoire")
    @Size(min = 8, max = 128, message = "Le mot de passe doit contenir entre 8 et 128 caractères")
    String password,

    @Schema(description = "Prénom", example = "Jean")
    @NotBlank(message = "Le prénom est obligatoire")
    @Size(max = 100)
    String firstName,

    @Schema(description = "Nom de famille", example = "Martin")
    @NotBlank(message = "Le nom est obligatoire")
    @Size(max = 100)
    String lastName,

    @Schema(description = "Rôle demandé", example = "ROLE_INVESTIGATOR")
    @NotBlank(message = "Le rôle est obligatoire")
    String roleName
) {}
