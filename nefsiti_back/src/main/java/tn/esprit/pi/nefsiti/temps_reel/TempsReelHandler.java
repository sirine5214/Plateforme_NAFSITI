package tn.esprit.pi.nefsiti.temps_reel;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.ConcurrentWebSocketSessionDecorator;
import org.springframework.web.socket.handler.TextWebSocketHandler;
import tn.esprit.pi.nefsiti.entities.Utilisateur;
import tn.esprit.pi.nefsiti.repositories.UtilisateurRepository;
import tn.esprit.pi.nefsiti.security.JwtService;
import tn.esprit.pi.nefsiti.security.TokenBlacklistService;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

import java.io.IOException;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Canal temps réel /ws (module 5) : le serveur pousse messages et alertes aux utilisateurs connectés.
 * Le client s'authentifie par un premier message {"type":"auth","token":"<JWT>"}
 * (le token ne passe pas dans l'URL, qui finit souvent dans les journaux d'accès).
 * L'envoi des messages se fait par l'API REST, qui applique la modération.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class TempsReelHandler extends TextWebSocketHandler {

    private static final String ATTR_UTILISATEUR = "utilisateurId";

    private final JwtService jwtService;
    private final TokenBlacklistService blacklist;
    private final UtilisateurRepository utilisateurRepository;
    private final JsonMapper jsonMapper;

    private final Map<Long, Set<WebSocketSession>> sessions = new ConcurrentHashMap<>();

    /** Événement poussé au client : type MESSAGE, ALERTE ou MODERATION. */
    public record Evenement(String type, Object donnees) {
    }

    @Override
    protected void handleTextMessage(WebSocketSession session, TextMessage message) throws IOException {
        if (session.getAttributes().containsKey(ATTR_UTILISATEUR)) {
            return; // déjà authentifié : le client n'envoie rien d'autre sur ce canal
        }
        Long id = authentifier(message.getPayload());
        if (id == null) {
            session.close(CloseStatus.POLICY_VIOLATION.withReason("Authentification refusée"));
            return;
        }
        session.getAttributes().put(ATTR_UTILISATEUR, id);
        sessions.computeIfAbsent(id, k -> ConcurrentHashMap.newKeySet())
                .add(new ConcurrentWebSocketSessionDecorator(session, 5_000, 256 * 1024));
        session.sendMessage(new TextMessage("{\"type\":\"PRET\"}"));
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) {
        Object id = session.getAttributes().get(ATTR_UTILISATEUR);
        if (id instanceof Long utilisateurId) {
            Set<WebSocketSession> ouvertes = sessions.get(utilisateurId);
            if (ouvertes != null) {
                ouvertes.removeIf(s -> s.getId().equals(session.getId()));
                if (ouvertes.isEmpty()) {
                    sessions.remove(utilisateurId);
                }
            }
        }
    }

    /** Pousse un événement à toutes les sessions ouvertes de l'utilisateur (sans effet s'il est hors ligne). */
    public void envoyer(Long utilisateurId, String type, Object donnees) {
        Set<WebSocketSession> ouvertes = sessions.get(utilisateurId);
        if (ouvertes == null || ouvertes.isEmpty()) {
            return;
        }
        TextMessage texte = new TextMessage(jsonMapper.writeValueAsString(new Evenement(type, donnees)));
        for (WebSocketSession s : ouvertes) {
            try {
                if (s.isOpen()) {
                    s.sendMessage(texte);
                }
            } catch (IOException | IllegalStateException e) {
                log.debug("Envoi temps réel impossible à la session {} : {}", s.getId(), e.getMessage());
            }
        }
    }

    private Long authentifier(String payload) {
        try {
            JsonNode json = jsonMapper.readTree(payload);
            if (!"auth".equals(json.path("type").asString(""))) {
                return null;
            }
            Claims claims = jwtService.lire(json.path("token").asString(""));
            if (blacklist.estRevoque(claims.getId()) || JwtService.estMfa(claims)) {
                return null;
            }
            Long id = Long.valueOf(claims.getSubject());
            return utilisateurRepository.findById(id).filter(Utilisateur::isActif).map(Utilisateur::getId).orElse(null);
        } catch (JwtException | IllegalArgumentException | tools.jackson.core.JacksonException e) {
            return null;
        }
    }
}
