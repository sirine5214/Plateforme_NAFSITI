package tn.esprit.pi.nefsiti.dto;

import tn.esprit.pi.nefsiti.entities.Utilisateur;

public record TherapeuteResponse(Long id, String nom, String prenom, String email, long creneauxLibres,
                                 String specialites, String approche, String langues) {

    public static TherapeuteResponse from(Utilisateur t, long creneauxLibres) {
        return new TherapeuteResponse(t.getId(), t.getNom(), t.getPrenom(), t.getEmail(), creneauxLibres,
                t.getSpecialites(), t.getApproche(), t.getLangues());
    }
}
