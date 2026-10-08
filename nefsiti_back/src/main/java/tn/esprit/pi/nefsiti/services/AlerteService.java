package tn.esprit.pi.nefsiti.services;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tn.esprit.pi.nefsiti.dto.AlerteResponse;
import tn.esprit.pi.nefsiti.dto.PersonneResume;
import tn.esprit.pi.nefsiti.entities.*;
import tn.esprit.pi.nefsiti.exceptions.ApiException;
import tn.esprit.pi.nefsiti.ia.IaClient;
import tn.esprit.pi.nefsiti.ia.IaIndisponibleException;
import tn.esprit.pi.nefsiti.repositories.AlerteRisqueRepository;
import tn.esprit.pi.nefsiti.repositories.RendezVousRepository;
import tn.esprit.pi.nefsiti.repositories.UtilisateurRepository;
import tn.esprit.pi.nefsiti.security.UtilisateurPrincipal;
import tn.esprit.pi.nefsiti.temps_reel.TempsReelHandler;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Module 6 — détection des signaux de détresse et alertes aux professionnels.
 * L'IA oriente et alerte ; la décision reste humaine (un thérapeute ou un administrateur traite l'alerte).
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AlerteService {

    /** À partir de ce niveau, une alerte est créée pour revue humaine. */
    public static final int NIVEAU_ALERTE = 2;

    private final AlerteRisqueRepository alerteRepository;
    private final UtilisateurRepository utilisateurRepository;
    private final RendezVousRepository rendezVousRepository;
    private final IaClient iaClient;
    private final TempsReelHandler tempsReel;
    private final EmailService emailService;

    /**
     * Analyse un texte du patient et crée une alerte si le niveau est élevé.
     *
     * @return le résultat de l'IA, ou null si le service est indisponible
     */
    @Transactional
    public IaClient.Risque analyser(Long patientId, String texte, SourceAlerte source) {
        IaClient.Risque risque;
        try {
            risque = iaClient.risque(iaClient.pseudonyme(patientId), texte);
        } catch (IaIndisponibleException e) {
            log.warn("Analyse de risque impossible (IA indisponible) pour une entrée {}", source);
            return null;
        }
        if (risque.niveau() >= NIVEAU_ALERTE) {
            creer(patientId, source, risque.niveau(), risque.probabilite(), risque.source());
        }
        return risque;
    }

    @Transactional
    public void creer(Long patientId, SourceAlerte source, int niveau, Double probabilite, String origine) {
        AlerteRisque alerte = alerteRepository.save(AlerteRisque.builder()
                .patient(utilisateurRepository.getReferenceById(patientId))
                .source(source)
                .niveau(niveau)
                .probabilite(probabilite)
                .origineDecision(origine)
                .dateCreation(LocalDateTime.now())
                .build());
        // Audit : décision tracée sans le texte (aucune donnée de santé brute dans les journaux).
        log.info("Alerte de risque niveau {} créée (source {}, décision {}) pour le patient {}",
                niveau, source, origine, patientId);
        notifier(alerte);
    }

    /** Thérapeute : alertes de ses patients ayant consenti · Administrateur : toutes. */
    @Transactional(readOnly = true)
    public List<AlerteResponse> lister(UtilisateurPrincipal user) {
        List<AlerteRisque> alertes = user.role() == Role.ADMINISTRATEUR
                ? alerteRepository.findAllByOrderByTraiteeAscDateCreationDesc()
                : alerteRepository.pourTherapeute(user.id(), StatutRendezVous.ANNULE);
        return alertes.stream().map(AlerteResponse::from).toList();
    }

    @Transactional
    public AlerteResponse traiter(UtilisateurPrincipal user, Long id) {
        AlerteRisque alerte = alerteRepository.findById(id)
                .orElseThrow(() -> ApiException.notFound("Alerte introuvable"));
        if (user.role() != Role.ADMINISTRATEUR && !estVisiblePar(alerte, user.id())) {
            throw new ApiException(HttpStatus.FORBIDDEN, "Cette alerte ne concerne pas l'un de vos patients");
        }
        if (!alerte.isTraitee()) {
            alerte.setTraitee(true);
            alerte.setTraiteePar(utilisateurRepository.getReferenceById(user.id()));
            alerte.setDateTraitement(LocalDateTime.now());
        }
        return AlerteResponse.from(alerte);
    }

    private boolean estVisiblePar(AlerteRisque alerte, Long therapeuteId) {
        Utilisateur patient = alerte.getPatient();
        return patient.isPartageAlertes()
                && rendezVousRepository.lienActif(patient.getId(), therapeuteId, StatutRendezVous.ANNULE);
    }

    /** Notification instantanée aux administrateurs et, si le patient l'a accepté, à ses thérapeutes. */
    private void notifier(AlerteRisque alerte) {
        Utilisateur patient = utilisateurRepository.findById(alerte.getPatient().getId()).orElseThrow();
        AlerteResponse vue = new AlerteResponse(alerte.getId(), PersonneResume.from(patient),
                alerte.getSource(), alerte.getNiveau(), alerte.getProbabilite(), alerte.getOrigineDecision(),
                alerte.getDateCreation(), false, null, null);
        utilisateurRepository.findByRoleAndActifTrueOrderByNomAscPrenomAsc(Role.ADMINISTRATEUR)
                .forEach(a -> tempsReel.envoyer(a.getId(), "ALERTE", vue));
        if (patient.isPartageAlertes()) {
            rendezVousRepository.therapeutesDuPatient(patient.getId(), StatutRendezVous.ANNULE).forEach(t -> {
                tempsReel.envoyer(t.getId(), "ALERTE", vue);
                emailService.nouvelleAlerte(t.getEmail(), t.getPrenom()); // utile si le thérapeute est hors ligne
            });
        }
    }
}
