package tn.esprit.pi.nefsiti.dto;

import jakarta.validation.constraints.*;
import tn.esprit.pi.nefsiti.entities.Role;

/** Création / modification par l'administrateur (mot de passe optionnel en modification). */
public record UtilisateurRequest(
        @NotBlank(message = "Le nom est obligatoire") @Size(max = 100) String nom,
        @NotBlank(message = "Le prénom est obligatoire") @Size(max = 100) String prenom,
        @NotBlank(message = "L'email est obligatoire") @Email(message = "Email invalide") @Size(max = 150) String email,
        String motDePasse,
        @NotNull(message = "Le rôle est obligatoire") Role role,
        Boolean actif
) {
}
