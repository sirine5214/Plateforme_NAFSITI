package tn.esprit.pi.nefsiti.controllers;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import tn.esprit.pi.nefsiti.dto.DisponibiliteRequest;
import tn.esprit.pi.nefsiti.dto.DisponibiliteResponse;
import tn.esprit.pi.nefsiti.dto.TherapeuteResponse;
import tn.esprit.pi.nefsiti.security.UtilisateurPrincipal;
import tn.esprit.pi.nefsiti.services.DisponibiliteService;

import java.util.List;

/** /api/disponibilites/** réservé au thérapeute ; /api/therapeutes/** à tout utilisateur connecté. */
@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class DisponibiliteController {

    private final DisponibiliteService service;

    // ===== Thérapeute =====

    @GetMapping("/disponibilites")
    public List<DisponibiliteResponse> mesCreneaux(@AuthenticationPrincipal UtilisateurPrincipal user) {
        return service.mesCreneaux(user.id());
    }

    @PostMapping("/disponibilites")
    public ResponseEntity<DisponibiliteResponse> ajouter(@AuthenticationPrincipal UtilisateurPrincipal user,
                                                         @Valid @RequestBody DisponibiliteRequest req) {
        return ResponseEntity.status(HttpStatus.CREATED).body(service.ajouter(user.id(), req));
    }

    @DeleteMapping("/disponibilites/{id}")
    public ResponseEntity<Void> supprimer(@AuthenticationPrincipal UtilisateurPrincipal user, @PathVariable Long id) {
        service.supprimer(user.id(), id);
        return ResponseEntity.noContent().build();
    }

    // ===== Consultation (patient) =====

    @GetMapping("/therapeutes")
    public List<TherapeuteResponse> therapeutes() {
        return service.therapeutes();
    }

    @GetMapping("/therapeutes/{id}/disponibilites")
    public List<DisponibiliteResponse> creneauxLibres(@PathVariable Long id) {
        return service.creneauxLibres(id);
    }
}
