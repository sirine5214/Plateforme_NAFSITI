package tn.esprit.pi.nefsiti.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

public record ChangementMotDePasseRequest(
        @NotBlank(message = "L'ancien mot de passe est obligatoire") String ancienMotDePasse,
        @NotBlank(message = "Le nouveau mot de passe est obligatoire")
        @Pattern(regexp = PasswordRules.PATTERN, message = PasswordRules.MESSAGE) String nouveauMotDePasse
) {
}
