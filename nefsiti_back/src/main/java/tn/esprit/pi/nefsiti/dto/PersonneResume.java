package tn.esprit.pi.nefsiti.dto;

import tn.esprit.pi.nefsiti.entities.Utilisateur;

/** Identité minimale d'un patient ou d'un thérapeute affichée dans un rendez-vous. */
public record PersonneResume(Long id, String nom, String prenom, String email) {

    public static PersonneResume from(Utilisateur u) {
        return new PersonneResume(u.getId(), u.getNom(), u.getPrenom(), u.getEmail());
    }
}
