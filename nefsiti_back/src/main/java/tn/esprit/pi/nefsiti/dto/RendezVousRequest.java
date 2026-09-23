package tn.esprit.pi.nefsiti.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record RendezVousRequest(
        @NotNull(message = "Le créneau est obligatoire") Long disponibiliteId,
        @Size(max = 500, message = "Le motif ne doit pas dépasser 500 caractères") String motif
) {
}
