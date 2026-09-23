package tn.esprit.pi.nefsiti.services;

import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tn.esprit.pi.nefsiti.dto.RendezVousRequest;
import tn.esprit.pi.nefsiti.dto.RendezVousResponse;
import tn.esprit.pi.nefsiti.entities.*;
import tn.esprit.pi.nefsiti.exceptions.ApiException;
import tn.esprit.pi.nefsiti.repositories.DisponibiliteRepository;
import tn.esprit.pi.nefsiti.repositories.RendezVousRepository;
import tn.esprit.pi.nefsiti.repositories.UtilisateurRepository;
import tn.esprit.pi.nefsiti.security.UtilisateurPrincipal;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class RendezVousService {

    private final RendezVousRepository rendezVousRepository;
    private final DisponibiliteRepository disponibiliteRepository;
    private final UtilisateurRepository utilisateurRepository;

    /** Patient : réserve un créneau libre -> rendez-vous EN_ATTENTE. */
    @Transactional
    public RendezVousResponse prendre(Long patientId, RendezVousRequest req) {
        Disponibilite creneau = disponibiliteRepository.findById(req.disponibiliteId())
                .orElseThrow(() -> ApiException.notFound("Créneau introuvable"));
        Utilisateur therapeute = creneau.getTherapeute();
        if (!therapeute.isActif()) {
            throw ApiException.badRequest("Ce thérapeute n'est plus disponible");
        }
        if (creneau.isReserve()) {
            throw ApiException.conflict("Ce créneau vient d'être réservé, choisissez-en un autre");
        }
        if (!creneau.getDebut().isAfter(LocalDateTime.now())) {
            throw ApiException.badRequest("Ce créneau est déjà passé");
        }
        if (rendezVousRepository.patientOccupe(patientId, creneau.getDebut(), creneau.getFin(), StatutRendezVous.ANNULE)) {
            throw ApiException.conflict("Vous avez déjà un rendez-vous sur cette plage horaire");
        }

        creneau.setReserve(true);
        disponibiliteRepository.saveAndFlush(creneau); // déclenche le verrou optimiste en cas de double réservation

        String motif = req.motif() == null || req.motif().isBlank() ? null : req.motif().trim();
        RendezVous rdv = rendezVousRepository.save(RendezVous.builder()
                .dateHeure(creneau.getDebut())
                .dateFin(creneau.getFin())
                .statut(StatutRendezVous.EN_ATTENTE)
                .motif(motif)
                .patient(utilisateurRepository.getReferenceById(patientId))
                .therapeute(therapeute)
                .disponibilite(creneau)
                .build());
        return RendezVousResponse.from(rdv);
    }

    /** Patient : ses rendez-vous · Thérapeute : ceux qu'il reçoit · Admin : tous. */
    @Transactional(readOnly = true)
    public List<RendezVousResponse> lister(UtilisateurPrincipal user) {
        List<RendezVous> liste = switch (user.role()) {
            case PATIENT -> rendezVousRepository.findByPatientIdOrderByDateHeureDesc(user.id());
            case THERAPEUTE -> rendezVousRepository.findByTherapeuteIdOrderByDateHeureDesc(user.id());
            case ADMINISTRATEUR -> rendezVousRepository.findAllByOrderByDateHeureDesc();
        };
        return liste.stream().map(RendezVousResponse::from).toList();
    }

    /** Thérapeute : confirme une demande en attente. */
    @Transactional
    public RendezVousResponse confirmer(Long therapeuteId, Long id) {
        RendezVous rdv = charger(id);
        if (!rdv.getTherapeute().getId().equals(therapeuteId)) {
            throw new ApiException(HttpStatus.FORBIDDEN, "Ce rendez-vous ne vous concerne pas");
        }
        if (rdv.getStatut() != StatutRendezVous.EN_ATTENTE) {
            throw ApiException.badRequest("Seul un rendez-vous en attente peut être confirmé");
        }
        if (!rdv.getDateHeure().isAfter(LocalDateTime.now())) {
            throw ApiException.badRequest("Ce rendez-vous est déjà passé");
        }
        rdv.setStatut(StatutRendezVous.CONFIRME);
        return RendezVousResponse.from(rdv);
    }

    /** Patient ou thérapeute concerné, ou administrateur : annule et libère le créneau. */
    @Transactional
    public RendezVousResponse annuler(UtilisateurPrincipal user, Long id) {
        RendezVous rdv = charger(id);
        boolean concerne = rdv.getPatient().getId().equals(user.id()) || rdv.getTherapeute().getId().equals(user.id());
        if (!concerne && user.role() != Role.ADMINISTRATEUR) {
            throw new ApiException(HttpStatus.FORBIDDEN, "Ce rendez-vous ne vous concerne pas");
        }
        if (rdv.getStatut() == StatutRendezVous.ANNULE) {
            throw ApiException.badRequest("Ce rendez-vous est déjà annulé");
        }
        if (!rdv.getDateHeure().isAfter(LocalDateTime.now())) {
            throw ApiException.badRequest("Un rendez-vous passé ne peut plus être annulé");
        }
        rdv.setStatut(StatutRendezVous.ANNULE);
        Disponibilite creneau = rdv.getDisponibilite();
        if (creneau != null) {
            creneau.setReserve(false); // le créneau redevient réservable
        }
        return RendezVousResponse.from(rdv);
    }

    private RendezVous charger(Long id) {
        return rendezVousRepository.findById(id).orElseThrow(() -> ApiException.notFound("Rendez-vous introuvable"));
    }
}
