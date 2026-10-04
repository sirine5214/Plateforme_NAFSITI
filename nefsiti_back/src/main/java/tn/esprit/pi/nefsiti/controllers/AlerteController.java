package tn.esprit.pi.nefsiti.controllers;

import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import tn.esprit.pi.nefsiti.dto.AlerteResponse;
import tn.esprit.pi.nefsiti.security.UtilisateurPrincipal;
import tn.esprit.pi.nefsiti.services.AlerteService;

import java.util.List;

/** Alertes de détresse : thérapeute (ses patients ayant consenti) et administrateur. */
@RestController
@RequestMapping("/api/alertes")
@RequiredArgsConstructor
public class AlerteController {

    private final AlerteService service;

    @GetMapping
    public List<AlerteResponse> lister(@AuthenticationPrincipal UtilisateurPrincipal user) {
        return service.lister(user);
    }

    @PatchMapping("/{id}/traiter")
    public AlerteResponse traiter(@AuthenticationPrincipal UtilisateurPrincipal user, @PathVariable Long id) {
        return service.traiter(user, id);
    }
}
