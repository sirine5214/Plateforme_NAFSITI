package tn.esprit.pi.nefsiti.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record MessageRequest(
        @NotNull(message = "Le destinataire est obligatoire") Long destinataireId,
        @NotBlank(message = "Le message est vide") @Size(max = 2000, message = "2000 caractères au maximum") String contenu
) {
}
