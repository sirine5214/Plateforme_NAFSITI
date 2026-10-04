package tn.esprit.pi.nefsiti.dto;

import tn.esprit.pi.nefsiti.entities.AlerteRisque;
import tn.esprit.pi.nefsiti.entities.SourceAlerte;

import java.time.LocalDateTime;

public record AlerteResponse(Long id, PersonneResume patient, SourceAlerte source, int niveau, Double probabilite,
                             String origineDecision, LocalDateTime dateCreation, boolean traitee,
                             PersonneResume traiteePar, LocalDateTime dateTraitement) {

    public static AlerteResponse from(AlerteRisque a) {
        return new AlerteResponse(a.getId(), PersonneResume.from(a.getPatient()), a.getSource(), a.getNiveau(),
                a.getProbabilite(), a.getOrigineDecision(), a.getDateCreation(), a.isTraitee(),
                a.getTraiteePar() == null ? null : PersonneResume.from(a.getTraiteePar()), a.getDateTraitement());
    }
}
