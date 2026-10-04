package tn.esprit.pi.nefsiti.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record MfaVisageRequest(
        @NotBlank(message = "Jeton de vérification manquant") String mfaToken,
        @NotBlank(message = "L'image est obligatoire") @Size(max = 3_000_000, message = "Image trop volumineuse") String image
) {
}
