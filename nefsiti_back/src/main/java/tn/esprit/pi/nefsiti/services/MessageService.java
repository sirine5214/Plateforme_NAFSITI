package tn.esprit.pi.nefsiti.services;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tn.esprit.pi.nefsiti.dto.*;
import tn.esprit.pi.nefsiti.entities.*;
import tn.esprit.pi.nefsiti.exceptions.ApiException;
import tn.esprit.pi.nefsiti.ia.IaClient;
import tn.esprit.pi.nefsiti.ia.IaIndisponibleException;
import tn.esprit.pi.nefsiti.repositories.MessageRepository;
import tn.esprit.pi.nefsiti.repositories.RendezVousRepository;
import tn.esprit.pi.nefsiti.repositories.UtilisateurRepository;
import tn.esprit.pi.nefsiti.security.UtilisateurPrincipal;
import tn.esprit.pi.nefsiti.temps_reel.TempsReelHandler;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Module 5 — messagerie sécurisée patient ↔ thérapeute, modérée par l'IA avant diffusion.
 * Priorité à la sécurité : un signal de crise n'empêche jamais la diffusion, il déclenche une escalade humaine.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class MessageService {

    public static final String PUBLIER = "PUBLIER";
    public static final String MASQUER = "MASQUER_EN_ATTENTE_REVUE";
    public static final String BLOQUER = "BLOQUER";
    public static final String ESCALADE = "ESCALADE_HUMAINE";
    public static final String NON_ANALYSE = "NON_ANALYSE";

    private final MessageRepository messageRepository;
    private final UtilisateurRepository utilisateurRepository;
    private final RendezVousRepository rendezVousRepository;
    private final AlerteService alerteService;
    private final IaClient iaClient;
    private final TempsReelHandler tempsReel;

    @Transactional(readOnly = true)
    public List<ContactResponse> contacts(UtilisateurPrincipal user) {
        List<Utilisateur> personnes = switch (user.role()) {
            case PATIENT -> rendezVousRepository.therapeutesDuPatient(user.id(), StatutRendezVous.ANNULE);
            case THERAPEUTE -> rendezVousRepository.patientsDuTherapeute(user.id(), StatutRendezVous.ANNULE);
            case ADMINISTRATEUR -> List.of();
        };
        return personnes.stream()
                .filter(Utilisateur::isActif)
                .map(p -> new ContactResponse(PersonneResume.from(p), p.getRole(),
                        messageRepository.countByDestinataireIdAndExpediteurIdAndLuFalseAndStatut(
                                user.id(), p.getId(), StatutMessage.PUBLIE)))
                .toList();
    }

    /** Fil de discussion ; les messages reçus sont marqués comme lus. */
    @Transactional
    public List<MessageResponse> conversation(UtilisateurPrincipal user, Long autreId) {
        verifierLien(user.id(), charger(autreId));
        messageRepository.marquerLus(user.id(), autreId);
        return messageRepository.conversation(user.id(), autreId, StatutMessage.PUBLIE).stream()
                .map(MessageResponse::from)
                .toList();
    }

    @Transactional
    public EnvoiMessageResponse envoyer(UtilisateurPrincipal user, MessageRequest req) {
        Utilisateur expediteur = charger(user.id());
        Utilisateur destinataire = charger(req.destinataireId());
        verifierLien(user.id(), destinataire);
        String texte = req.contenu().trim();

        IaClient.Moderation moderation = null;
        try {
            moderation = iaClient.moderer(iaClient.pseudonyme(user.id()), texte);
        } catch (IaIndisponibleException e) {
            log.warn("Modération indisponible : message publié sans analyse");
        }
        String decision = moderation == null ? NON_ANALYSE : moderation.decision();
        // Journal d'audit des décisions de modération (sans le contenu)
        log.info("Modération message {} -> {} : {} (toxicité {})", user.id(), destinataire.getId(), decision,
                moderation == null ? null : moderation.toxicite());

        if (BLOQUER.equals(decision)) {
            throw ApiException.badRequest("Ce message n'a pas été envoyé : il contient des propos jugés inappropriés.");
        }

        Message message = messageRepository.save(Message.builder()
                .expediteur(expediteur)
                .destinataire(destinataire)
                .contenu(texte)
                .dateEnvoi(LocalDateTime.now())
                .statut(MASQUER.equals(decision) ? StatutMessage.MASQUE : StatutMessage.PUBLIE)
                .decisionModeration(decision)
                .toxicite(moderation == null ? null : moderation.toxicite())
                .niveauRisque(moderation == null ? null : moderation.niveauRisque())
                .build());

        boolean escalade = ESCALADE.equals(decision);
        if (escalade && expediteur.getRole() == Role.PATIENT) {
            alerteService.creer(expediteur.getId(), SourceAlerte.MESSAGE, moderation.niveauRisque(), null, "moderation");
        }
        MessageResponse vue = MessageResponse.from(message);
        if (message.getStatut() == StatutMessage.PUBLIE) {
            tempsReel.envoyer(destinataire.getId(), "MESSAGE", vue);
        }
        return new EnvoiMessageResponse(vue, decision, escalade);
    }

    // ===== Revue humaine (administrateur) =====

    @Transactional(readOnly = true)
    public List<MessageModerationResponse> enAttente() {
        return messageRepository.findByStatutOrderByDateEnvoiAsc(StatutMessage.MASQUE).stream()
                .map(MessageModerationResponse::from)
                .toList();
    }

    @Transactional
    public void decider(Long id, boolean publier) {
        Message m = messageRepository.findById(id).orElseThrow(() -> ApiException.notFound("Message introuvable"));
        if (m.getStatut() != StatutMessage.MASQUE) {
            throw ApiException.badRequest("Ce message a déjà été traité");
        }
        m.setStatut(publier ? StatutMessage.PUBLIE : StatutMessage.REJETE);
        log.info("Revue humaine du message {} : {}", id, m.getStatut());
        MessageResponse vue = MessageResponse.from(m);
        if (publier) {
            tempsReel.envoyer(m.getDestinataire().getId(), "MESSAGE", vue);
        }
        tempsReel.envoyer(m.getExpediteur().getId(), "MODERATION", vue);
    }

    /** Messagerie réservée aux couples patient / thérapeute ayant au moins un rendez-vous non annulé. */
    private void verifierLien(Long moiId, Utilisateur autre) {
        Utilisateur moi = charger(moiId);
        boolean autorise = !autre.getId().equals(moiId) && autre.isActif() && (
                (moi.getRole() == Role.PATIENT && autre.getRole() == Role.THERAPEUTE
                        && rendezVousRepository.lienActif(moiId, autre.getId(), StatutRendezVous.ANNULE))
                || (moi.getRole() == Role.THERAPEUTE && autre.getRole() == Role.PATIENT
                        && rendezVousRepository.lienActif(autre.getId(), moiId, StatutRendezVous.ANNULE)));
        if (!autorise) {
            throw new ApiException(HttpStatus.FORBIDDEN,
                    "La messagerie est ouverte entre un patient et un thérapeute ayant un rendez-vous en commun");
        }
    }

    private Utilisateur charger(Long id) {
        return utilisateurRepository.findById(id).orElseThrow(() -> ApiException.notFound("Utilisateur introuvable"));
    }
}
