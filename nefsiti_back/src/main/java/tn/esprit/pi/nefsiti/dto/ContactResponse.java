package tn.esprit.pi.nefsiti.dto;

import tn.esprit.pi.nefsiti.entities.Role;

/** Interlocuteur possible : thérapeute du patient, ou patient du thérapeute (rendez-vous non annulé). */
public record ContactResponse(PersonneResume personne, Role role, long nonLus) {
}
