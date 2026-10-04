package tn.esprit.pi.nefsiti.dto;

/** @param publier vrai : le message est diffusé ; faux : il est rejeté. */
public record DecisionModerationRequest(boolean publier) {
}
