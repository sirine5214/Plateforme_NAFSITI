package tn.esprit.pi.nefsiti.dto;

import tn.esprit.pi.nefsiti.entities.Utilisateur;

/** @param noteMoyenne moyenne des avis des patients (null s'il n'y en a pas encore) */
public record TherapeuteResponse(Long id, String nom, String prenom, String email, long creneauxLibres,
                                 String specialites, String approche, String langues,
                                 Double noteMoyenne, long nombreAvis) {

    public static TherapeuteResponse from(Utilisateur t, long creneauxLibres, Double noteMoyenne, long nombreAvis) {
        return new TherapeuteResponse(t.getId(), t.getNom(), t.getPrenom(), t.getEmail(), creneauxLibres,
                t.getSpecialites(), t.getApproche(), t.getLangues(),
                noteMoyenne == null ? null : Math.round(noteMoyenne * 10) / 10.0, nombreAvis);
    }
}
