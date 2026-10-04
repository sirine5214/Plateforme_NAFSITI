package tn.esprit.pi.nefsiti.dto;

import java.util.List;

/** Tendance de l'humeur sur 30 jours (statut : amelioration, stable, degradation, historique_insuffisant, indisponible). */
public record TendanceResponse(String statut, Double pente, Double moyenne7j, Double moyenne30j,
                               boolean baisseSignificative, List<PointHumeur> points) {
}
