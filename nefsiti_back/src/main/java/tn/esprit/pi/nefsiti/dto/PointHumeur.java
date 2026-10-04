package tn.esprit.pi.nefsiti.dto;

import java.time.LocalDate;

/** Moyenne quotidienne de l'humeur (1-10) et de la valence du texte (1-5). */
public record PointHumeur(LocalDate date, double humeur, Double valence) {
}
