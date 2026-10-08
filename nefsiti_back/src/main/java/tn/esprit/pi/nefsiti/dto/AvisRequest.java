package tn.esprit.pi.nefsiti.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;

public record AvisRequest(
        @Min(value = 1, message = "La note va de 1 à 5") @Max(value = 5, message = "La note va de 1 à 5") int note,
        @Size(max = 1000, message = "1000 caractères au maximum") String commentaire
) {
}
