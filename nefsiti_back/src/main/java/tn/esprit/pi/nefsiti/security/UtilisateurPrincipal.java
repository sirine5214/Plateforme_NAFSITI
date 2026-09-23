package tn.esprit.pi.nefsiti.security;

import tn.esprit.pi.nefsiti.entities.Role;

/** Utilisateur authentifié par JWT, injectable via @AuthenticationPrincipal. */
public record UtilisateurPrincipal(Long id, String email, Role role) {
}
