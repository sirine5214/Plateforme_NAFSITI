package tn.esprit.pi.nefsiti.dto;

import tn.esprit.pi.nefsiti.entities.RendezVous;
import tn.esprit.pi.nefsiti.entities.StatutRendezVous;

import java.time.LocalDateTime;

public record RendezVousResponse(
        Long id,
        LocalDateTime dateHeure,
        LocalDateTime dateFin,
        StatutRendezVous statut,
        String motif,
        PersonneResume patient,
        PersonneResume therapeute,
        LocalDateTime dateCreation,
        Integer noteAvis,
        String commentaireAvis
) {
    public static RendezVousResponse from(RendezVous r) {
        return new RendezVousResponse(r.getId(), r.getDateHeure(), r.getDateFin(), r.getStatut(), r.getMotif(),
                PersonneResume.from(r.getPatient()), PersonneResume.from(r.getTherapeute()), r.getDateCreation(),
                r.getNoteAvis(), r.getCommentaireAvis());
    }
}
