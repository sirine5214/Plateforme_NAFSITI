package tn.esprit.pi.nefsiti.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;

import java.util.List;

public record JournalRequest(
        @Min(value = 1, message = "L'humeur va de 1 à 10") @Max(value = 10, message = "L'humeur va de 1 à 10") int humeur,
        @Size(max = 5000, message = "5000 caractères au maximum") String texte,
        @Size(max = 10, message = "10 tags au maximum") List<@Size(max = 30) String> tags
) {
}
