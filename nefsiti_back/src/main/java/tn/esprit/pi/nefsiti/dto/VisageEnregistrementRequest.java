package tn.esprit.pi.nefsiti.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;

import java.util.List;

/** Captures webcam (base64) servant à calculer l'empreinte faciale de référence. */
public record VisageEnregistrementRequest(
        @NotEmpty(message = "Au moins une capture est nécessaire")
        @Size(max = 5, message = "5 captures au maximum")
        List<@NotBlank @Size(max = 3_000_000, message = "Image trop volumineuse") String> images
) {
}
