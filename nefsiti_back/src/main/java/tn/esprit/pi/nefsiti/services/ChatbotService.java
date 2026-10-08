package tn.esprit.pi.nefsiti.services;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tn.esprit.pi.nefsiti.dto.ChatbotResponse;
import tn.esprit.pi.nefsiti.entities.Role;
import tn.esprit.pi.nefsiti.entities.SourceAlerte;
import tn.esprit.pi.nefsiti.ia.IaClient;
import tn.esprit.pi.nefsiti.ia.IaIndisponibleException;
import tn.esprit.pi.nefsiti.security.UtilisateurPrincipal;

import java.util.List;

/**
 * Chatbot d'orientation : aucune conversation n'est enregistrée. Seul un signal de détresse d'un patient
 * crée une alerte (sans le texte), comme pour le journal et la messagerie.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ChatbotService {

    /** Réponse de secours si le service IA est indisponible : elle oriente sans analyser. */
    private static final ChatbotResponse SECOURS = new ChatbotResponse(
            "Je ne peux pas analyser votre message pour le moment. Vous pouvez prendre rendez-vous, consulter les "
                    + "ressources ou écrire à votre thérapeute. En cas d'urgence, appelez le 3114 (24 h/24) ou le 15.",
            List.of(new ChatbotResponse.Action("Prendre rendez-vous", "/rendez-vous/prendre"),
                    new ChatbotResponse.Action("Ressources bien-être", "/ressources"),
                    new ChatbotResponse.Action("Appeler le 3114", "tel:3114")),
            0);

    private final IaClient iaClient;
    private final AlerteService alerteService;

    @Transactional
    public ChatbotResponse repondre(UtilisateurPrincipal user, String message) {
        IaClient.ReponseChatbot r;
        try {
            r = iaClient.chatbot(iaClient.pseudonyme(user.id()), message.trim());
        } catch (IaIndisponibleException e) {
            return SECOURS;
        }
        if (r.niveauRisque() >= AlerteService.NIVEAU_ALERTE && user.role() == Role.PATIENT) {
            alerteService.creer(user.id(), SourceAlerte.CHATBOT, r.niveauRisque(), null, "chatbot");
        }
        return new ChatbotResponse(r.reponse(),
                r.actions().stream().map(a -> new ChatbotResponse.Action(a.libelle(), a.lien())).toList(),
                r.niveauRisque());
    }
}
