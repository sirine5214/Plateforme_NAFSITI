package tn.esprit.pi.nefsiti.dto;

import jakarta.validation.constraints.*;
import tn.esprit.pi.nefsiti.entities.Role;

public record RegisterRequest(
        @NotBlank(message = "Le nom est obligatoire") @Size(max = 100) String nom,
        @NotBlank(message = "Le prénom est obligatoire") @Size(max = 100) String prenom,
        @NotBlank(message = "L'email est obligatoire") @Email(message = "Email invalide") @Size(max = 150) String email,
        @NotBlank(message = "Le mot de passe est obligatoire")
        @Pattern(regexp = PasswordRules.PATTERN, message = PasswordRules.MESSAGE) String motDePasse,
        @NotNull(message = "Le rôle est obligatoire") Role role,
        @AssertTrue(message = "Vous devez accepter le traitement de vos données pour créer un compte") boolean consentement
) {
}
