package tn.esprit.pi.nefsiti.dto;

import jakarta.validation.constraints.NotNull;

public record StatutRequest(@NotNull(message = "Le statut est obligatoire") Boolean actif) {
}
