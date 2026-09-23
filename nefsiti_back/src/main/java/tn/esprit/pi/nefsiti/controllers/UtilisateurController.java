package tn.esprit.pi.nefsiti.controllers;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import tn.esprit.pi.nefsiti.dto.*;
import tn.esprit.pi.nefsiti.entities.Role;
import tn.esprit.pi.nefsiti.security.UtilisateurPrincipal;
import tn.esprit.pi.nefsiti.services.UtilisateurService;

import java.util.List;

/** /api/utilisateurs/** réservé à l'administrateur, sauf /me (voir SecurityConfig). */
@RestController
@RequestMapping("/api/utilisateurs")
@RequiredArgsConstructor
public class UtilisateurController {

    private final UtilisateurService service;

    // ===== Profil connecté =====

    @PutMapping("/me")
    public UtilisateurResponse modifierProfil(@AuthenticationPrincipal UtilisateurPrincipal principal,
                                              @Valid @RequestBody ProfilRequest req) {
        return service.modifierProfil(principal.id(), req);
    }

    @PutMapping("/me/mot-de-passe")
    public ResponseEntity<Void> changerMotDePasse(@AuthenticationPrincipal UtilisateurPrincipal principal,
                                                  @Valid @RequestBody ChangementMotDePasseRequest req) {
        service.changerMotDePasse(principal.id(), req);
        return ResponseEntity.noContent().build();
    }

    // ===== Administration =====

    @GetMapping
    public List<UtilisateurResponse> lister(@RequestParam(required = false) String recherche,
                                            @RequestParam(required = false) Role role,
                                            @RequestParam(required = false) Boolean actif) {
        return service.lister(recherche, role, actif);
    }

    @GetMapping("/stats")
    public UtilisateurStats statistiques() {
        return service.statistiques();
    }

    @GetMapping("/{id}")
    public UtilisateurResponse trouver(@PathVariable Long id) {
        return service.trouver(id);
    }

    @PostMapping
    public ResponseEntity<UtilisateurResponse> creer(@Valid @RequestBody UtilisateurRequest req) {
        return ResponseEntity.status(HttpStatus.CREATED).body(service.creer(req));
    }

    @PutMapping("/{id}")
    public UtilisateurResponse modifier(@PathVariable Long id, @Valid @RequestBody UtilisateurRequest req,
                                        @AuthenticationPrincipal UtilisateurPrincipal admin) {
        return service.modifier(id, req, admin.id());
    }

    @PatchMapping("/{id}/statut")
    public UtilisateurResponse changerStatut(@PathVariable Long id, @Valid @RequestBody StatutRequest req,
                                             @AuthenticationPrincipal UtilisateurPrincipal admin) {
        return service.changerStatut(id, req.actif(), admin.id());
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> supprimer(@PathVariable Long id,
                                          @AuthenticationPrincipal UtilisateurPrincipal admin) {
        service.supprimer(id, admin.id());
        return ResponseEntity.noContent().build();
    }
}
