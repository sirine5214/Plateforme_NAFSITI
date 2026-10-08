package tn.esprit.pi.nefsiti.ia;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import tn.esprit.pi.nefsiti.exceptions.ApiException;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.time.Duration;
import java.util.HexFormat;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Client du service IA Python (FastAPI), protégé par la clé d'API interne.
 * Les utilisateurs sont pseudonymisés : seul un identifiant opaque est transmis, jamais nom ni email.
 */
@Slf4j
@Component
public class IaClient {

    private final RestClient http;
    private final SecretKeySpec clePseudonymisation;

    public IaClient(@Value("${app.ia.url}") String url,
                    @Value("${app.ia.api-key}") String apiKey,
                    @Value("${app.ia.timeout-ms}") long timeoutMs) {
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(Duration.ofSeconds(2));
        factory.setReadTimeout(Duration.ofMillis(timeoutMs));
        this.clePseudonymisation = new SecretKeySpec(apiKey.getBytes(StandardCharsets.UTF_8), "HmacSHA256");
        this.http = RestClient.builder()
                .baseUrl(url)
                .requestFactory(factory)
                .defaultHeader("X-API-Key", apiKey)
                .build();
    }

    // ===== Contrat de réponse (champs en snake_case côté Python) =====

    public record EvaluationConnexion(@JsonProperty("score_risque") double scoreRisque, String action) {
    }

    public record TherapeuteClasse(@JsonProperty("therapeute_id") long therapeuteId, double score, double similarite) {
    }

    public record Valence(double valence) {
    }

    public record Tendance(String statut, Double pente,
                           @JsonProperty("moyenne_7j") Double moyenne7j,
                           @JsonProperty("moyenne_30j") Double moyenne30j,
                           @JsonProperty("baisse_significative") boolean baisseSignificative) {
    }

    public record ContenuRecommande(@JsonProperty("contenu_id") long contenuId, Double score) {
    }

    public record Recommandations(List<ContenuRecommande> contenus,
                                  @JsonProperty("heure_rappel") int heureRappel,
                                  boolean personnalise) {
    }

    public record Moderation(double toxicite, @JsonProperty("niveau_risque") int niveauRisque, String decision) {
    }

    public record Risque(int niveau, Double probabilite, String source, List<String> recommandations) {
    }

    public record ActionChatbot(String libelle, String lien) {
    }

    public record ReponseChatbot(String reponse, List<ActionChatbot> actions, String intention,
                                 @JsonProperty("niveau_risque") int niveauRisque, double confiance) {
    }

    public record Enrolement(List<Double> empreinte, int captures, String modele) {
    }

    public record Verification(boolean correspond, double distance, double seuil) {
    }

    /** Donnée envoyée au matching pour chaque thérapeute. */
    public record ProfilTherapeute(long id, String specialites, String approche, String langues,
                                   double noteMoyenne, long creneaux7j) {
    }

    /** Donnée envoyée à la recommandation pour chaque ressource. */
    public record Contenu(long id, String titre, String description, int popularite, boolean valideParPro) {
    }

    // ===== Module 1 — Comptes & sécurité =====

    public EvaluationConnexion evaluerConnexion(int heure, boolean nouvelAppareil, boolean paysDifferent,
                                                long echecs24h, double distanceKm) {
        return post("/v1/connexion/evaluer", Map.of(
                "heure_connexion", heure,
                "nouvel_appareil", nouvelAppareil ? 1 : 0,
                "pays_different", paysDifferent ? 1 : 0,
                "echecs_24h", echecs24h,
                "distance_km_derniere_ip", distanceKm), EvaluationConnexion.class);
    }

    public void reentrainerConnexion(List<double[]> historique) {
        post("/v1/connexion/entrainer", Map.of("historique", historique), Map.class);
    }

    // ===== Module 2 — Rendez-vous =====

    public List<TherapeuteClasse> classerTherapeutes(String besoin, String langue, List<ProfilTherapeute> therapeutes) {
        List<Map<String, Object>> profils = therapeutes.stream().map(t -> Map.<String, Object>of(
                "id", t.id(),
                "specialites", nonNull(t.specialites()),
                "approche", nonNull(t.approche()),
                "langues", nonNull(t.langues()),
                "note_moyenne", t.noteMoyenne(),
                "creneaux_7j", t.creneaux7j())).toList();
        return List.of(post("/v1/therapeutes/classer",
                Map.of("besoin", besoin, "langue", langue, "therapeutes", profils), TherapeuteClasse[].class));
    }

    // ===== Module 3 — Journal & humeur =====

    public Valence valence(String userId, String texte) {
        return post("/v1/journal/valence", Map.of("user_id", userId, "texte", texte), Valence.class);
    }

    public Tendance tendance(List<Double> humeurs) {
        return post("/v1/journal/tendance", Map.of("humeurs", humeurs), Tendance.class);
    }

    // ===== Module 4 — Ressources =====

    public Recommandations recommander(String userId, List<String> entreesJournal, List<String> contenusAimes,
                                       List<Contenu> catalogue, List<Long> dejaVus, List<Integer> heuresOuverture) {
        List<Map<String, Object>> contenus = catalogue.stream().map(c -> Map.<String, Object>of(
                "id", c.id(),
                "titre", c.titre(),
                "description", nonNull(c.description()),
                "popularite", c.popularite(),
                "valide_par_pro", c.valideParPro())).toList();
        Map<String, Object> corps = new LinkedHashMap<>();
        corps.put("user_id", userId);
        corps.put("entrees_journal", entreesJournal);
        corps.put("contenus_aimes", contenusAimes);
        corps.put("catalogue", contenus);
        corps.put("deja_vus", dejaVus);
        corps.put("heures_ouverture", heuresOuverture);
        corps.put("k", 5);
        return post("/v1/ressources/recommander", corps, Recommandations.class);
    }

    // ===== Module 5 — Messagerie =====

    public Moderation moderer(String userId, String texte) {
        return post("/v1/messages/moderer", Map.of("user_id", userId, "texte", texte), Moderation.class);
    }

    // ===== Module 6 — Détection de risque =====

    public Risque risque(String userId, String texte) {
        return post("/v1/risque", Map.of("user_id", userId, "texte", texte), Risque.class);
    }

    // ===== Chatbot d'orientation =====

    public ReponseChatbot chatbot(String userId, String texte) {
        return post("/v1/chatbot", Map.of("user_id", userId, "texte", texte), ReponseChatbot.class);
    }

    // ===== Reconnaissance faciale =====

    public Enrolement enrolerVisage(String userId, List<String> images) {
        return post("/v1/visage/enroler", Map.of("user_id", userId, "images", images), Enrolement.class);
    }

    public Verification verifierVisage(String userId, String image, List<Double> reference) {
        return post("/v1/visage/verifier", Map.of("user_id", userId, "image", image, "reference", reference),
                Verification.class);
    }

    // ===== Pseudonymisation =====

    /** Identifiant opaque (HMAC de l'id) transmis à l'IA à la place de l'id réel. */
    public String pseudonyme(Long utilisateurId) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(clePseudonymisation);
            byte[] h = mac.doFinal(String.valueOf(utilisateurId).getBytes(StandardCharsets.UTF_8));
            return "u-" + HexFormat.of().formatHex(h, 0, 8);
        } catch (GeneralSecurityException e) {
            throw new IllegalStateException(e);
        }
    }

    private <T> T post(String chemin, Object corps, Class<T> type) {
        try {
            T reponse = http.post().uri(chemin)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(corps)
                    .retrieve()
                    .body(type);
            if (reponse == null) {
                throw new IaIndisponibleException("Réponse vide du service IA (" + chemin + ")", null);
            }
            return reponse;
        } catch (HttpClientErrorException e) {
            if (e.getStatusCode().value() == HttpStatus.UNPROCESSABLE_ENTITY.value()) {
                // Erreur métier explicite (ex. aucun visage détecté) : renvoyée telle quelle à l'utilisateur
                throw ApiException.badRequest(detail(e));
            }
            throw new IaIndisponibleException("Service IA : " + e.getStatusCode() + " sur " + chemin, e);
        } catch (RestClientException e) {
            log.warn("Service IA indisponible ({}) : {}", chemin, e.getMessage());
            throw new IaIndisponibleException("Service IA injoignable", e);
        }
    }

    private static String detail(HttpClientErrorException e) {
        try {
            Map<?, ?> corps = e.getResponseBodyAs(Map.class);
            if (corps != null && corps.get("detail") instanceof String s) {
                return s;
            }
        } catch (RuntimeException ignored) {
            // corps illisible : message générique
        }
        return "Image refusée par le service de reconnaissance faciale";
    }

    private static String nonNull(String s) {
        return s == null ? "" : s;
    }
}
