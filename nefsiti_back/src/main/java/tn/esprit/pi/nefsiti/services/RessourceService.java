package tn.esprit.pi.nefsiti.services;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tn.esprit.pi.nefsiti.dto.RecommandationsResponse;
import tn.esprit.pi.nefsiti.dto.RessourceRequest;
import tn.esprit.pi.nefsiti.dto.RessourceResponse;
import tn.esprit.pi.nefsiti.entities.EntreeJournal;
import tn.esprit.pi.nefsiti.entities.InteractionRessource;
import tn.esprit.pi.nefsiti.entities.Ressource;
import tn.esprit.pi.nefsiti.exceptions.ApiException;
import tn.esprit.pi.nefsiti.ia.IaClient;
import tn.esprit.pi.nefsiti.ia.IaIndisponibleException;
import tn.esprit.pi.nefsiti.repositories.*;

import java.time.LocalDateTime;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

/** Module 4 — bibliothèque de ressources et recommandation personnalisée. */
@Service
@RequiredArgsConstructor
public class RessourceService {

    private static final int HEURE_RAPPEL_DEFAUT = 20;

    private final RessourceRepository ressourceRepository;
    private final InteractionRessourceRepository interactionRepository;
    private final EntreeJournalRepository journalRepository;
    private final TentativeConnexionRepository tentativeRepository;
    private final UtilisateurRepository utilisateurRepository;
    private final IaClient iaClient;

    @Transactional(readOnly = true)
    public List<RessourceResponse> lister(Long utilisateurId) {
        Map<Long, InteractionRessource> interactions = interactions(utilisateurId);
        return ressourceRepository.findAllByOrderByTypeAscTitreAsc().stream()
                .map(r -> vue(r, interactions.get(r.getId())))
                .toList();
    }

    /** Ouvre une ressource : compte la consultation et l'ajoute à l'historique de l'utilisateur. */
    @Transactional
    public RessourceResponse consulter(Long utilisateurId, Long id) {
        Ressource r = charger(id);
        r.setPopularite(r.getPopularite() + 1);
        InteractionRessource i = interaction(utilisateurId, r);
        i.setDerniereConsultation(LocalDateTime.now());
        interactionRepository.save(i);
        return vue(r, i);
    }

    @Transactional
    public RessourceResponse aimer(Long utilisateurId, Long id, boolean aime) {
        Ressource r = charger(id);
        InteractionRessource i = interaction(utilisateurId, r);
        i.setAime(aime);
        interactionRepository.save(i);
        return vue(r, i);
    }

    /** 5 contenus recommandés à partir du journal récent et des contenus aimés (similarité sémantique). */
    @Transactional(readOnly = true)
    public RecommandationsResponse recommandations(Long utilisateurId) {
        List<Ressource> catalogue = ressourceRepository.findAll();
        Map<Long, InteractionRessource> interactions = interactions(utilisateurId);
        Map<Long, Ressource> parId = catalogue.stream().collect(Collectors.toMap(Ressource::getId, Function.identity()));

        List<String> journal = journalRepository.findTop10ByPatientIdOrderByDateCreationDesc(utilisateurId).stream()
                .map(EntreeJournal::getTexte)
                .filter(Objects::nonNull)
                .toList();
        List<String> aimes = interactions.values().stream()
                .filter(InteractionRessource::isAime)
                .map(i -> i.getRessource().getTitre() + " " + i.getRessource().getDescription())
                .toList();
        List<Integer> heures = tentativeRepository.datesConnexion(utilisateurId, LocalDateTime.now().minusDays(60))
                .stream().map(LocalDateTime::getHour).toList();

        try {
            IaClient.Recommandations reco = iaClient.recommander(iaClient.pseudonyme(utilisateurId), journal, aimes,
                    catalogue.stream().map(r -> new IaClient.Contenu(r.getId(), r.getTitre(), r.getDescription(),
                            r.getPopularite(), r.isValideParPro())).toList(),
                    new ArrayList<>(interactions.keySet()), heures);
            List<RessourceResponse> ressources = reco.contenus().stream()
                    .map(c -> parId.get(c.contenuId()))
                    .filter(Objects::nonNull)
                    .map(r -> vue(r, interactions.get(r.getId())))
                    .toList();
            return new RecommandationsResponse(ressources, reco.heureRappel(), reco.personnalise());
        } catch (IaIndisponibleException e) {
            // Mode dégradé : contenus les plus populaires
            List<RessourceResponse> populaires = catalogue.stream()
                    .sorted(Comparator.comparingInt(Ressource::getPopularite).reversed())
                    .limit(5)
                    .map(r -> vue(r, interactions.get(r.getId())))
                    .toList();
            return new RecommandationsResponse(populaires, HEURE_RAPPEL_DEFAUT, false);
        }
    }

    // ===== Administration =====

    @Transactional
    public RessourceResponse creer(RessourceRequest req) {
        Ressource r = new Ressource();
        appliquer(r, req);
        return vue(ressourceRepository.save(r), null);
    }

    @Transactional
    public RessourceResponse modifier(Long id, RessourceRequest req) {
        Ressource r = charger(id);
        appliquer(r, req);
        return vue(r, null);
    }

    @Transactional
    public void supprimer(Long id) {
        Ressource r = charger(id);
        interactionRepository.supprimerParRessource(id);
        ressourceRepository.delete(r);
    }

    private void appliquer(Ressource r, RessourceRequest req) {
        r.setTitre(req.titre().trim());
        r.setDescription(req.description().trim());
        r.setType(req.type());
        r.setContenu(req.contenu() == null || req.contenu().isBlank() ? null : req.contenu().trim());
        r.setDureeMinutes(req.dureeMinutes());
        r.setUrl(req.url() == null || req.url().isBlank() ? null : req.url().trim());
        r.setValideParPro(req.valideParPro());
    }

    private Map<Long, InteractionRessource> interactions(Long utilisateurId) {
        return interactionRepository.findByUtilisateurId(utilisateurId).stream()
                .collect(Collectors.toMap(i -> i.getRessource().getId(), Function.identity()));
    }

    private InteractionRessource interaction(Long utilisateurId, Ressource r) {
        return interactionRepository.findByUtilisateurIdAndRessourceId(utilisateurId, r.getId())
                .orElseGet(() -> InteractionRessource.builder()
                        .utilisateur(utilisateurRepository.getReferenceById(utilisateurId))
                        .ressource(r)
                        .derniereConsultation(LocalDateTime.now())
                        .build());
    }

    private static RessourceResponse vue(Ressource r, InteractionRessource i) {
        return RessourceResponse.from(r, i != null && i.isAime(), i != null);
    }

    private Ressource charger(Long id) {
        return ressourceRepository.findById(id).orElseThrow(() -> ApiException.notFound("Ressource introuvable"));
    }
}
