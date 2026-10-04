package tn.esprit.pi.nefsiti.controllers;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import tn.esprit.pi.nefsiti.dto.*;
import tn.esprit.pi.nefsiti.security.UtilisateurPrincipal;
import tn.esprit.pi.nefsiti.services.MessageService;

import java.util.List;

/**
 * Messagerie patient ↔ thérapeute. L'envoi passe par REST (modération IA), la réception
 * est poussée en temps réel sur le WebSocket /ws.
 */
@RestController
@RequiredArgsConstructor
public class MessageController {

    private final MessageService service;

    @GetMapping("/api/messages/contacts")
    public List<ContactResponse> contacts(@AuthenticationPrincipal UtilisateurPrincipal user) {
        return service.contacts(user);
    }

    @GetMapping("/api/messages/{autreId}")
    public List<MessageResponse> conversation(@AuthenticationPrincipal UtilisateurPrincipal user,
                                              @PathVariable Long autreId) {
        return service.conversation(user, autreId);
    }

    @PostMapping("/api/messages")
    public ResponseEntity<EnvoiMessageResponse> envoyer(@AuthenticationPrincipal UtilisateurPrincipal user,
                                                        @Valid @RequestBody MessageRequest req) {
        return ResponseEntity.status(HttpStatus.CREATED).body(service.envoyer(user, req));
    }

    // ===== Revue humaine (administrateur) =====

    @GetMapping("/api/moderation/messages")
    public List<MessageModerationResponse> enAttente() {
        return service.enAttente();
    }

    @PatchMapping("/api/moderation/messages/{id}")
    public ResponseEntity<Void> decider(@PathVariable Long id, @RequestBody DecisionModerationRequest req) {
        service.decider(id, req.publier());
        return ResponseEntity.noContent().build();
    }
}
