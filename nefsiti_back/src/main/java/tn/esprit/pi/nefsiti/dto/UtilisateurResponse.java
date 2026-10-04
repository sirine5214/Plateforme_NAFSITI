package tn.esprit.pi.nefsiti.dto;

import tn.esprit.pi.nefsiti.entities.Role;
import tn.esprit.pi.nefsiti.entities.Utilisateur;

import java.time.LocalDateTime;

/** Vue publique d'un utilisateur (jamais le mot de passe). */
public record UtilisateurResponse(
        Long id,
        String nom,
        String prenom,
        String email,
        Role role,
        boolean actif,
        LocalDateTime dateCreation,
        LocalDateTime dateConsentement,
        boolean partageAlertes,
        String specialites,
        String approche,
        String langues
) {
    public static UtilisateurResponse from(Utilisateur u) {
        return new UtilisateurResponse(u.getId(), u.getNom(), u.getPrenom(), u.getEmail(),
                u.getRole(), u.isActif(), u.getDateCreation(), u.getDateConsentement(), u.isPartageAlertes(),
                u.getSpecialites(), u.getApproche(), u.getLangues());
    }
}
