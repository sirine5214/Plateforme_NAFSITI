package tn.esprit.pi.nefsiti.dto;

import jakarta.validation.constraints.*;
import tn.esprit.pi.nefsiti.entities.TypeRessource;

public record RessourceRequest(
        @NotBlank(message = "Le titre est obligatoire") @Size(max = 150) String titre,
        @NotBlank(message = "La description est obligatoire") @Size(max = 500) String description,
        @NotNull(message = "Le type est obligatoire") TypeRessource type,
        @Size(max = 20000) String contenu,
        @Min(1) @Max(240) Integer dureeMinutes,
        @Size(max = 500) @Pattern(regexp = "^$|^https?://.+", message = "Lien http(s) invalide") String url,
        boolean valideParPro
) {
}
