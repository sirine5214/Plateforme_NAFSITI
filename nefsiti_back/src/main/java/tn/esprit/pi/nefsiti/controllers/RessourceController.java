package tn.esprit.pi.nefsiti.controllers;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import tn.esprit.pi.nefsiti.dto.RecommandationsResponse;
import tn.esprit.pi.nefsiti.dto.RessourceRequest;
import tn.esprit.pi.nefsiti.dto.RessourceResponse;
import tn.esprit.pi.nefsiti.security.UtilisateurPrincipal;
import tn.esprit.pi.nefsiti.services.RessourceService;

import java.util.List;

/** Consultation : tout utilisateur connecté · Création / modification / suppression : administrateur. */
@RestController
@RequestMapping("/api/ressources")
@RequiredArgsConstructor
public class RessourceController {

    private final RessourceService service;

    @GetMapping
    public List<RessourceResponse> lister(@AuthenticationPrincipal UtilisateurPrincipal user) {
        return service.lister(user.id());
    }

    @GetMapping("/recommandations")
    public RecommandationsResponse recommandations(@AuthenticationPrincipal UtilisateurPrincipal user) {
        return service.recommandations(user.id());
    }

    @PostMapping("/{id}/consulter")
    public RessourceResponse consulter(@AuthenticationPrincipal UtilisateurPrincipal user, @PathVariable Long id) {
        return service.consulter(user.id(), id);
    }

    @PutMapping("/{id}/aime")
    public RessourceResponse aimer(@AuthenticationPrincipal UtilisateurPrincipal user, @PathVariable Long id,
                                   @RequestParam boolean valeur) {
        return service.aimer(user.id(), id, valeur);
    }

    @PostMapping
    public ResponseEntity<RessourceResponse> creer(@Valid @RequestBody RessourceRequest req) {
        return ResponseEntity.status(HttpStatus.CREATED).body(service.creer(req));
    }

    @PutMapping("/{id}")
    public RessourceResponse modifier(@PathVariable Long id, @Valid @RequestBody RessourceRequest req) {
        return service.modifier(id, req);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> supprimer(@PathVariable Long id) {
        service.supprimer(id);
        return ResponseEntity.noContent().build();
    }
}
