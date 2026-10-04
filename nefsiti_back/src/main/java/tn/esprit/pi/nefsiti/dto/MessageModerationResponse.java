package tn.esprit.pi.nefsiti.dto;

import tn.esprit.pi.nefsiti.entities.Message;

import java.time.LocalDateTime;

/** Message masqué en attente de revue humaine. */
public record MessageModerationResponse(Long id, PersonneResume expediteur, PersonneResume destinataire,
                                        String contenu, LocalDateTime dateEnvoi, Double toxicite) {

    public static MessageModerationResponse from(Message m) {
        return new MessageModerationResponse(m.getId(), PersonneResume.from(m.getExpediteur()),
                PersonneResume.from(m.getDestinataire()), m.getContenu(), m.getDateEnvoi(), m.getToxicite());
    }
}
