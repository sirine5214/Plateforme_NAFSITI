package tn.esprit.pi.nefsiti.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record ChatbotRequest(
        @NotBlank(message = "Le message est vide") @Size(max = 1000, message = "1000 caractères au maximum") String message
) {
}
