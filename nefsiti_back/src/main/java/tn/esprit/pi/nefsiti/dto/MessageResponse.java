package tn.esprit.pi.nefsiti.dto;

import tn.esprit.pi.nefsiti.entities.Message;
import tn.esprit.pi.nefsiti.entities.StatutMessage;

import java.time.LocalDateTime;

public record MessageResponse(Long id, Long expediteurId, Long destinataireId, String contenu,
                              LocalDateTime dateEnvoi, StatutMessage statut, boolean lu) {

    public static MessageResponse from(Message m) {
        return new MessageResponse(m.getId(), m.getExpediteur().getId(), m.getDestinataire().getId(), m.getContenu(),
                m.getDateEnvoi(), m.getStatut(), m.isLu());
    }
}
