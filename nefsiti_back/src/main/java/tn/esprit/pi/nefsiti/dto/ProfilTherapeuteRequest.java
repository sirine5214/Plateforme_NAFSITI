package tn.esprit.pi.nefsiti.dto;

import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record ProfilTherapeuteRequest(
        @Size(max = 500) String specialites,
        @Size(max = 500) String approche,
        @Size(max = 50) @Pattern(regexp = "^$|^[a-zA-Z]{2}(\s*,\s*[a-zA-Z]{2})*$",
                message = "Codes de langue séparés par des virgules, ex. fr,ar,en") String langues
) {
}
