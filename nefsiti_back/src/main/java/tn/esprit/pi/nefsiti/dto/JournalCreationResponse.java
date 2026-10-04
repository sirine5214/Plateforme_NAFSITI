package tn.esprit.pi.nefsiti.dto;

import java.util.List;

/**
 * Entrée enregistrée + retour immédiat de l'IA : niveau de risque (0-3), codes de recommandation
 * (urgences_3114, respiration_guidee…) et baisse d'humeur significative.
 */
public record JournalCreationResponse(JournalResponse entree, int niveauRisque, List<String> recommandations,
                                      boolean baisseSignificative) {
}
