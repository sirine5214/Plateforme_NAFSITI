package tn.esprit.pi.nefsiti.controllers;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import tn.esprit.pi.nefsiti.dto.JournalCreationResponse;
import tn.esprit.pi.nefsiti.dto.JournalRequest;
import tn.esprit.pi.nefsiti.dto.JournalResponse;
import tn.esprit.pi.nefsiti.dto.TendanceResponse;
import tn.esprit.pi.nefsiti.security.UtilisateurPrincipal;
import tn.esprit.pi.nefsiti.services.JournalService;

import java.util.List;

/** Journal d'humeur personnel : réservé au patient (voir SecurityConfig). */
@RestController
@RequestMapping("/api/journal")
@RequiredArgsConstructor
public class JournalController {

    private final JournalService service;

    @GetMapping
    public List<JournalResponse> lister(@AuthenticationPrincipal UtilisateurPrincipal user) {
        return service.lister(user.id());
    }

    @PostMapping
    public ResponseEntity<JournalCreationResponse> creer(@AuthenticationPrincipal UtilisateurPrincipal user,
                                                         @Valid @RequestBody JournalRequest req) {
        return ResponseEntity.status(HttpStatus.CREATED).body(service.creer(user.id(), req));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> supprimer(@AuthenticationPrincipal UtilisateurPrincipal user, @PathVariable Long id) {
        service.supprimer(user.id(), id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/tendance")
    public TendanceResponse tendance(@AuthenticationPrincipal UtilisateurPrincipal user) {
        return service.tendance(user.id());
    }
}
