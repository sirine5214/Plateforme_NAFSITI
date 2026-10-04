package tn.esprit.pi.nefsiti.services;

import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tn.esprit.pi.nefsiti.dto.*;
import tn.esprit.pi.nefsiti.entities.EntreeJournal;
import tn.esprit.pi.nefsiti.entities.SourceAlerte;
import tn.esprit.pi.nefsiti.exceptions.ApiException;
import tn.esprit.pi.nefsiti.ia.IaClient;
import tn.esprit.pi.nefsiti.ia.IaIndisponibleException;
import tn.esprit.pi.nefsiti.repositories.EntreeJournalRepository;
import tn.esprit.pi.nefsiti.repositories.UtilisateurRepository;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Module 3 — journal et suivi de l'humeur. Chaque entrée est analysée par l'IA :
 * valence du texte (DistilCamemBERT), risque (module 6) et tendance sur 30 jours.
 * Si le service IA est indisponible, l'entrée est tout de même enregistrée.
 */
@Service
@RequiredArgsConstructor
public class JournalService {

    private static final List<String> CONTENUS_SOUTIEN = List.of("respiration_guidee", "meditation_courte");

    private final EntreeJournalRepository journalRepository;
    private final UtilisateurRepository utilisateurRepository;
    private final AlerteService alerteService;
    private final IaClient iaClient;

    @Transactional(readOnly = true)
    public List<JournalResponse> lister(Long patientId) {
        return journalRepository.findByPatientIdOrderByDateCreationDesc(patientId).stream()
                .map(JournalResponse::from)
                .toList();
    }

    @Transactional
    public JournalCreationResponse creer(Long patientId, JournalRequest req) {
        String texte = req.texte() == null || req.texte().isBlank() ? null : req.texte().trim();
        String tags = req.tags() == null ? null : req.tags().stream()
                .map(t -> t.trim().toLowerCase().replace(",", " "))
                .filter(t -> !t.isEmpty())
                .distinct()
                .collect(Collectors.joining(","));

        EntreeJournal entree = EntreeJournal.builder()
                .patient(utilisateurRepository.getReferenceById(patientId))
                .humeur(req.humeur())
                .texte(texte)
                .tags(tags == null || tags.isEmpty() ? null : tags)
                .build();

        int niveau = 0;
        List<String> recommandations = new ArrayList<>();
        if (texte != null) {
            try {
                entree.setValence(iaClient.valence(iaClient.pseudonyme(patientId), texte).valence());
            } catch (IaIndisponibleException ignored) {
                // valence facultative
            }
            IaClient.Risque risque = alerteService.analyser(patientId, texte, SourceAlerte.JOURNAL);
            if (risque != null) {
                niveau = risque.niveau();
                entree.setNiveauRisque(niveau);
                recommandations.addAll(risque.recommandations());
            }
        }
        journalRepository.save(entree);

        boolean baisse = tendance(patientId).baisseSignificative();
        if (baisse) {
            CONTENUS_SOUTIEN.stream().filter(c -> !recommandations.contains(c)).forEach(recommandations::add);
        }
        return new JournalCreationResponse(JournalResponse.from(entree), niveau, recommandations, baisse);
    }

    @Transactional
    public void supprimer(Long patientId, Long id) {
        EntreeJournal e = journalRepository.findById(id)
                .orElseThrow(() -> ApiException.notFound("Entrée introuvable"));
        if (!e.getPatient().getId().equals(patientId)) {
            throw new ApiException(HttpStatus.FORBIDDEN, "Cette entrée ne vous appartient pas");
        }
        journalRepository.delete(e);
    }

    /** Moyennes quotidiennes sur 30 jours et tendance calculée par régression linéaire (côté Python). */
    @Transactional(readOnly = true)
    public TendanceResponse tendance(Long patientId) {
        List<EntreeJournal> entrees = journalRepository.findByPatientIdAndDateCreationAfterOrderByDateCreationAsc(
                patientId, LocalDateTime.now().minusDays(30));
        Map<LocalDate, List<EntreeJournal>> parJour = entrees.stream()
                .collect(Collectors.groupingBy(e -> e.getDateCreation().toLocalDate(), TreeMap::new, Collectors.toList()));
        List<PointHumeur> points = parJour.entrySet().stream()
                .map(j -> new PointHumeur(j.getKey(),
                        arrondi(j.getValue().stream().mapToInt(EntreeJournal::getHumeur).average().orElse(0)),
                        moyenneValence(j.getValue())))
                .toList();
        try {
            IaClient.Tendance t = iaClient.tendance(points.stream().map(PointHumeur::humeur).toList());
            return new TendanceResponse(t.statut(), t.pente(), t.moyenne7j(), t.moyenne30j(),
                    t.baisseSignificative(), points);
        } catch (IaIndisponibleException e) {
            return new TendanceResponse("indisponible", null, null, null, false, points);
        }
    }

    private static Double moyenneValence(List<EntreeJournal> entrees) {
        OptionalDouble m = entrees.stream().map(EntreeJournal::getValence).filter(Objects::nonNull)
                .mapToDouble(Double::doubleValue).average();
        return m.isPresent() ? arrondi(m.getAsDouble()) : null;
    }

    private static double arrondi(double v) {
        return Math.round(v * 100) / 100.0;
    }
}
