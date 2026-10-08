package tn.esprit.pi.nefsiti.controllers;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import tn.esprit.pi.nefsiti.dto.ChatbotRequest;
import tn.esprit.pi.nefsiti.dto.ChatbotResponse;
import tn.esprit.pi.nefsiti.security.UtilisateurPrincipal;
import tn.esprit.pi.nefsiti.services.ChatbotService;

/** Chatbot d'orientation, ouvert à tout utilisateur connecté. */
@RestController
@RequestMapping("/api/chatbot")
@RequiredArgsConstructor
public class ChatbotController {

    private final ChatbotService service;

    @PostMapping
    public ChatbotResponse repondre(@AuthenticationPrincipal UtilisateurPrincipal user,
                                    @Valid @RequestBody ChatbotRequest req) {
        return service.repondre(user, req.message());
    }
}
