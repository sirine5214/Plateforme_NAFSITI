package tn.esprit.pi.nefsiti.dto;

import java.util.List;

/**
 * @param personnalise faux en cas de démarrage à froid ou d'IA indisponible (contenus populaires)
 * @param heureRappel  heure (0-23) à laquelle l'utilisateur ouvre le plus souvent l'application
 */
public record RecommandationsResponse(List<RessourceResponse> ressources, int heureRappel, boolean personnalise) {
}
