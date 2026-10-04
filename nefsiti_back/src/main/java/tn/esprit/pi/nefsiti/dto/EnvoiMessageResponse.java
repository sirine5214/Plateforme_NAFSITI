package tn.esprit.pi.nefsiti.dto;

/**
 * @param decision        décision de la modération (PUBLIER, MASQUER_EN_ATTENTE_REVUE, ESCALADE_HUMAINE, NON_ANALYSE)
 * @param afficherUrgence signal de crise détecté : afficher les numéros d'urgence à l'expéditeur
 */
public record EnvoiMessageResponse(MessageResponse message, String decision, boolean afficherUrgence) {
}
