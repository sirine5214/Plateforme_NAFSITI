package tn.esprit.pi.nefsiti.controllers;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import tn.esprit.pi.nefsiti.dto.VisageEnregistrementRequest;
import tn.esprit.pi.nefsiti.dto.VisageStatutResponse;
import tn.esprit.pi.nefsiti.security.UtilisateurPrincipal;
import tn.esprit.pi.nefsiti.services.VisageService;

/** Gestion de sa propre empreinte faciale (tout utilisateur connecté). */
@RestController
@RequestMapping("/api/utilisateurs/me/visage")
@RequiredArgsConstructor
public class VisageController {

    private final VisageService service;

    @GetMapping
    public VisageStatutResponse statut(@AuthenticationPrincipal UtilisateurPrincipal user) {
        return service.statut(user.id());
    }

    @PostMapping
    public VisageStatutResponse enregistrer(@AuthenticationPrincipal UtilisateurPrincipal user,
                                            @Valid @RequestBody VisageEnregistrementRequest req) {
        return service.enregistrer(user.id(), req.images());
    }

    @DeleteMapping
    public ResponseEntity<Void> supprimer(@AuthenticationPrincipal UtilisateurPrincipal user) {
        service.supprimer(user.id());
        return ResponseEntity.noContent().build();
    }
}
