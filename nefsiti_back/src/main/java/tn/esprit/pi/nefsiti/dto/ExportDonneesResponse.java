package tn.esprit.pi.nefsiti.dto;

import java.time.LocalDateTime;
import java.util.List;

/** Droit d'accès / portabilité (RGPD art. 15 et 20) : les données personnelles de l'utilisateur. */
public record ExportDonneesResponse(LocalDateTime dateExport, UtilisateurResponse profil, boolean visageEnregistre,
                                    List<JournalResponse> journal, List<RendezVousResponse> rendezVous,
                                    List<MessageResponse> messages) {
}
