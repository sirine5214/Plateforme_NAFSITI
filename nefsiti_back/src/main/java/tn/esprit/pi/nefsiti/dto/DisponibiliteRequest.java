package tn.esprit.pi.nefsiti.dto;

import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDateTime;

public record DisponibiliteRequest(
        @NotNull(message = "La date et l'heure sont obligatoires")
        @Future(message = "Le créneau doit être dans le futur") LocalDateTime debut,
        @NotNull(message = "La durée est obligatoire")
        @Min(value = 15, message = "La durée minimale est de 15 minutes")
        @Max(value = 240, message = "La durée maximale est de 4 heures") Integer dureeMinutes
) {
}
