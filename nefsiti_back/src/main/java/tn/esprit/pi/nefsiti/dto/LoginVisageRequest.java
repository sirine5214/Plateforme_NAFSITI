package tn.esprit.pi.nefsiti.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/** Connexion par reconnaissance faciale : l'email désigne le compte, l'image le confirme (vérification 1:1). */
public record LoginVisageRequest(
        @NotBlank(message = "L'email est obligatoire") @Email(message = "Email invalide") String email,
        @NotBlank(message = "L'image est obligatoire") @Size(max = 3_000_000, message = "Image trop volumineuse") String image
) {
}
