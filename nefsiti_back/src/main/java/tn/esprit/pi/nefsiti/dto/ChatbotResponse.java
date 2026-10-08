package tn.esprit.pi.nefsiti.dto;

import java.util.List;

/**
 * Réponse du chatbot d'orientation.
 *
 * @param actions      liens proposés : route de l'application (/ressources…) ou numéro (tel:3114)
 * @param niveauRisque 0-3 ; à partir de 2, l'interface met en avant les numéros d'urgence
 */
public record ChatbotResponse(String reponse, List<Action> actions, int niveauRisque) {

    public record Action(String libelle, String lien) {
    }
}
