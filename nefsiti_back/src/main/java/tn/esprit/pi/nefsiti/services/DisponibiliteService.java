package tn.esprit.pi.nefsiti.services;

import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tn.esprit.pi.nefsiti.dto.DisponibiliteRequest;
import tn.esprit.pi.nefsiti.dto.DisponibiliteResponse;
import tn.esprit.pi.nefsiti.dto.TherapeuteResponse;
import tn.esprit.pi.nefsiti.entities.Disponibilite;
import tn.esprit.pi.nefsiti.entities.Role;
import tn.esprit.pi.nefsiti.entities.Utilisateur;
import tn.esprit.pi.nefsiti.exceptions.ApiException;
import tn.esprit.pi.nefsiti.repositories.DisponibiliteRepository;
import tn.esprit.pi.nefsiti.repositories.RendezVousRepository;
import tn.esprit.pi.nefsiti.repositories.UtilisateurRepository;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class DisponibiliteService {

    private final DisponibiliteRepository disponibiliteRepository;
    private final RendezVousRepository rendezVousRepository;
    private final UtilisateurRepository utilisateurRepository;

    // ===== Thérapeute : gérer ses disponibilités =====

    @Transactional(readOnly = true)
    public List<DisponibiliteResponse> mesCreneaux(Long therapeuteId) {
        return disponibiliteRepository.findByTherapeuteIdAndDebutAfterOrderByDebutAsc(therapeuteId, LocalDateTime.now())
                .stream().map(DisponibiliteResponse::from).toList();
    }

    @Transactional
    public DisponibiliteResponse ajouter(Long therapeuteId, DisponibiliteRequest req) {
        LocalDateTime debut = req.debut().withSecond(0).withNano(0);
        LocalDateTime fin = debut.plusMinutes(req.dureeMinutes());
        if (disponibiliteRepository.chevauche(therapeuteId, debut, fin)) {
            throw ApiException.conflict("Ce créneau chevauche une disponibilité existante");
        }
        Utilisateur therapeute = utilisateurRepository.getReferenceById(therapeuteId);
        Disponibilite d = disponibiliteRepository.save(Disponibilite.builder()
                .therapeute(therapeute)
                .debut(debut)
                .fin(fin)
                .reserve(false)
                .build());
        return DisponibiliteResponse.from(d);
    }

    @Transactional
    public void supprimer(Long therapeuteId, Long id) {
        Disponibilite d = disponibiliteRepository.findById(id)
                .orElseThrow(() -> ApiException.notFound("Créneau introuvable"));
        if (!d.getTherapeute().getId().equals(therapeuteId)) {
            throw new ApiException(HttpStatus.FORBIDDEN, "Ce créneau ne vous appartient pas");
        }
        if (d.isReserve()) {
            throw ApiException.badRequest("Ce créneau est réservé : annulez d'abord le rendez-vous associé");
        }
        rendezVousRepository.detacherDisponibilite(id); // anciens rendez-vous annulés sur ce créneau
        disponibiliteRepository.delete(d);
    }

    // ===== Patient : choisir un thérapeute et un créneau =====

    @Transactional(readOnly = true)
    public List<TherapeuteResponse> therapeutes() {
        LocalDateTime maintenant = LocalDateTime.now();
        return utilisateurRepository.findByRoleAndActifTrueOrderByNomAscPrenomAsc(Role.THERAPEUTE).stream()
                .map(t -> new TherapeuteResponse(t.getId(), t.getNom(), t.getPrenom(), t.getEmail(),
                        disponibiliteRepository.countByTherapeuteIdAndReserveFalseAndDebutAfter(t.getId(), maintenant)))
                .toList();
    }

    @Transactional(readOnly = true)
    public List<DisponibiliteResponse> creneauxLibres(Long therapeuteId) {
        return disponibiliteRepository
                .findByTherapeuteIdAndReserveFalseAndDebutAfterOrderByDebutAsc(therapeuteId, LocalDateTime.now())
                .stream().map(DisponibiliteResponse::from).toList();
    }
}
