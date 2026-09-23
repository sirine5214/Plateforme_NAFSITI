package tn.esprit.pi.nefsiti.dto;

import tn.esprit.pi.nefsiti.entities.Disponibilite;

import java.time.LocalDateTime;

public record DisponibiliteResponse(Long id, LocalDateTime debut, LocalDateTime fin, boolean reserve) {

    public static DisponibiliteResponse from(Disponibilite d) {
        return new DisponibiliteResponse(d.getId(), d.getDebut(), d.getFin(), d.isReserve());
    }
}
