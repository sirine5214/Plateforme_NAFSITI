package tn.esprit.pi.nefsiti.dto;

/** Suggestion du matching IA : le patient garde le libre choix dans l'annuaire. */
public record TherapeuteRecommandeResponse(TherapeuteResponse therapeute, double score, double similarite) {
}
