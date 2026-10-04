package tn.esprit.pi.nefsiti.services;

import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tn.esprit.pi.nefsiti.dto.VisageStatutResponse;
import tn.esprit.pi.nefsiti.entities.EmpreinteFaciale;
import tn.esprit.pi.nefsiti.exceptions.ApiException;
import tn.esprit.pi.nefsiti.ia.IaClient;
import tn.esprit.pi.nefsiti.ia.IaIndisponibleException;
import tn.esprit.pi.nefsiti.repositories.EmpreinteFacialeRepository;
import tn.esprit.pi.nefsiti.repositories.UtilisateurRepository;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

/** Reconnaissance faciale : enregistrement de l'empreinte et vérification (calculs TensorFlow côté Python). */
@Service
@RequiredArgsConstructor
public class VisageService {

    private final EmpreinteFacialeRepository empreinteRepository;
    private final UtilisateurRepository utilisateurRepository;
    private final IaClient iaClient;

    @Transactional(readOnly = true)
    public VisageStatutResponse statut(Long utilisateurId) {
        return empreinteRepository.findByUtilisateurId(utilisateurId)
                .map(e -> new VisageStatutResponse(true, e.getDateEnregistrement()))
                .orElse(new VisageStatutResponse(false, null));
    }

    @Transactional(readOnly = true)
    public boolean estEnregistre(Long utilisateurId) {
        return empreinteRepository.existsByUtilisateurId(utilisateurId);
    }

    /** Calcule l'empreinte de référence à partir de plusieurs captures et remplace l'éventuelle précédente. */
    @Transactional
    public VisageStatutResponse enregistrer(Long utilisateurId, List<String> images) {
        IaClient.Enrolement enrolement;
        try {
            enrolement = iaClient.enrolerVisage(iaClient.pseudonyme(utilisateurId), images);
        } catch (IaIndisponibleException e) {
            throw indisponible();
        }
        String vecteur = enrolement.empreinte().stream().map(String::valueOf).collect(Collectors.joining(","));
        EmpreinteFaciale empreinte = empreinteRepository.findByUtilisateurId(utilisateurId)
                .orElseGet(() -> EmpreinteFaciale.builder()
                        .utilisateur(utilisateurRepository.getReferenceById(utilisateurId))
                        .build());
        empreinte.setVecteur(vecteur);
        empreinte.setModele(enrolement.modele());
        empreinte.setDateEnregistrement(LocalDateTime.now());
        empreinteRepository.save(empreinte);
        return new VisageStatutResponse(true, empreinte.getDateEnregistrement());
    }

    @Transactional
    public void supprimer(Long utilisateurId) {
        empreinteRepository.supprimerParUtilisateur(utilisateurId);
    }

    /**
     * @return vrai si l'image correspond à l'empreinte enregistrée.
     * @throws ApiException 401 si aucune empreinte n'est enregistrée, 400 si l'image est inexploitable,
     *                      503 si le service IA est indisponible.
     */
    @Transactional(readOnly = true)
    public boolean correspond(Long utilisateurId, String image) {
        EmpreinteFaciale empreinte = empreinteRepository.findByUtilisateurId(utilisateurId)
                .orElseThrow(VisageService::nonDisponible);
        List<Double> reference = Arrays.stream(empreinte.getVecteur().split(","))
                .map(Double::valueOf)
                .toList();
        try {
            return iaClient.verifierVisage(iaClient.pseudonyme(utilisateurId), image, reference).correspond();
        } catch (IaIndisponibleException e) {
            throw indisponible();
        }
    }

    /** Même message que pour un email inconnu : ne révèle pas quels comptes ont activé le visage. */
    static ApiException nonDisponible() {
        return new ApiException(HttpStatus.UNAUTHORIZED,
                "Connexion par reconnaissance faciale impossible pour ce compte. Utilisez votre mot de passe.");
    }

    private static ApiException indisponible() {
        return new ApiException(HttpStatus.SERVICE_UNAVAILABLE,
                "La reconnaissance faciale est momentanément indisponible. Utilisez votre mot de passe.");
    }
}
