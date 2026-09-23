package tn.esprit.pi.nefsiti.controllers;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import tn.esprit.pi.nefsiti.dto.RendezVousRequest;
import tn.esprit.pi.nefsiti.dto.RendezVousResponse;
import tn.esprit.pi.nefsiti.security.UtilisateurPrincipal;
import tn.esprit.pi.nefsiti.services.RendezVousService;

import java.util.List;

/** Règles d'accès par rôle dans SecurityConfig ; appartenance vérifiée dans RendezVousService. */
@RestController
@RequestMapping("/api/rendez-vous")
@RequiredArgsConstructor
public class RendezVousController {

    private final RendezVousService service;

    @GetMapping
    public List<RendezVousResponse> lister(@AuthenticationPrincipal UtilisateurPrincipal user) {
        return service.lister(user);
    }

    @PostMapping
    public ResponseEntity<RendezVousResponse> prendre(@AuthenticationPrincipal UtilisateurPrincipal user,
                                                      @Valid @RequestBody RendezVousRequest req) {
        return ResponseEntity.status(HttpStatus.CREATED).body(service.prendre(user.id(), req));
    }

    @PatchMapping("/{id}/confirmer")
    public RendezVousResponse confirmer(@AuthenticationPrincipal UtilisateurPrincipal user, @PathVariable Long id) {
        return service.confirmer(user.id(), id);
    }

    @PatchMapping("/{id}/annuler")
    public RendezVousResponse annuler(@AuthenticationPrincipal UtilisateurPrincipal user, @PathVariable Long id) {
        return service.annuler(user, id);
    }
}
