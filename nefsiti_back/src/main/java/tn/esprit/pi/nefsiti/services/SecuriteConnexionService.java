package tn.esprit.pi.nefsiti.services;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import tn.esprit.pi.nefsiti.entities.MethodeConnexion;
import tn.esprit.pi.nefsiti.entities.TentativeConnexion;
import tn.esprit.pi.nefsiti.exceptions.ApiException;
import tn.esprit.pi.nefsiti.ia.IaClient;
import tn.esprit.pi.nefsiti.ia.IaIndisponibleException;
import tn.esprit.pi.nefsiti.repositories.TentativeConnexionRepository;
import tn.esprit.pi.nefsiti.repositories.UtilisateurRepository;
import tn.esprit.pi.nefsiti.security.ContexteConnexion;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.LocalDateTime;
import java.util.HexFormat;
import java.util.List;

/**
 * Module 1 — détection des connexions anormales (IsolationForest côté Python) et journal des accès.
 * Les écritures se font dans une transaction séparée pour être conservées même si la connexion échoue.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class SecuriteConnexionService {

    public static final String AUTORISER = "AUTORISER";
    public static final String MFA_RENFORCEE = "MFA_RENFORCEE";
    public static final String BLOQUER = "BLOQUER_ET_ALERTER";
    public static final String ECHEC_MOT_DE_PASSE = "MOT_DE_PASSE_INCORRECT";
    public static final String ECHEC_VISAGE = "VISAGE_NON_RECONNU";
    private static final List<String> ECHECS = List.of(ECHEC_MOT_DE_PASSE, ECHEC_VISAGE);

    private final TentativeConnexionRepository tentativeRepository;
    private final UtilisateurRepository utilisateurRepository;
    private final IaClient iaClient;

    @Value("${app.securite.blocage-minutes}")
    private long blocageMinutes;

    /** @param mfaExigee second facteur (visage) à demander avant de délivrer le token. */
    public record Decision(String action, Double score, boolean mfaExigee) {
        public boolean bloquer() {
            return BLOQUER.equals(action);
        }
    }

    /** Refuse la connexion pendant le blocage temporaire qui suit une connexion jugée très suspecte. */
    @Transactional(readOnly = true)
    public void verifierNonBloque(Long utilisateurId) {
        if (tentativeRepository.existsByUtilisateurIdAndActionAndDateTentativeAfter(
                utilisateurId, BLOQUER, LocalDateTime.now().minusMinutes(blocageMinutes))) {
            throw erreurBlocage();
        }
    }

    public ApiException erreurBlocage() {
        return new ApiException(HttpStatus.LOCKED, "Connexion inhabituelle détectée : votre compte est "
                + "temporairement bloqué. Réessayez dans " + blocageMinutes + " minutes.");
    }

    /**
     * Évalue la connexion (identifiants déjà vérifiés) et la journalise.
     *
     * @param mfaPossible l'utilisateur a enregistré son visage et s'est connecté par mot de passe : un
     *                    second facteur peut être exigé. Sinon une connexion « MFA » est acceptée (et tracée).
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public Decision evaluerEtEnregistrer(Long utilisateurId, MethodeConnexion methode, ContexteConnexion ctx,
                                         boolean mfaPossible) {
        String appareil = empreinteAppareil(ctx.userAgent());
        LocalDateTime maintenant = LocalDateTime.now();
        // Premier accès : pas d'historique, l'appareil n'est donc pas considéré comme « nouveau ».
        boolean nouvelAppareil = tentativeRepository.existsByUtilisateurIdAndSuccesTrue(utilisateurId)
                && !tentativeRepository.existsByUtilisateurIdAndSuccesTrueAndAppareil(utilisateurId, appareil);
        long echecs = tentativeRepository.compterEchecs(utilisateurId, ECHECS, maintenant.minusHours(24));

        Decision decision;
        try {
            // Pas de géolocalisation IP pour l'instant : pays et distance à 0 (à brancher sur une base GeoIP).
            IaClient.EvaluationConnexion e = iaClient.evaluerConnexion(maintenant.getHour(), nouvelAppareil,
                    false, echecs, 0);
            decision = new Decision(e.action(), e.scoreRisque(), MFA_RENFORCEE.equals(e.action()) && mfaPossible);
        } catch (IaIndisponibleException e) {
            // Disponibilité avant tout : la connexion reste possible, mais c'est tracé.
            log.warn("Évaluation de connexion impossible (IA indisponible), connexion autorisée : utilisateur {}",
                    utilisateurId);
            decision = new Decision(AUTORISER, null, false);
        }

        tentativeRepository.save(TentativeConnexion.builder()
                .utilisateur(utilisateurRepository.getReferenceById(utilisateurId))
                .dateTentative(maintenant)
                .methode(methode)
                .succes(!decision.bloquer() && !decision.mfaExigee())
                .ip(ctx.ip())
                .appareil(appareil)
                .nouvelAppareil(nouvelAppareil)
                .echecs24h((int) echecs)
                .scoreRisque(decision.score())
                .action(decision.action())
                .build());

        if (decision.bloquer()) {
            // TODO brancher l'envoi d'un e-mail d'alerte quand un serveur SMTP sera configuré
            log.warn("ALERTE SÉCURITÉ : connexion bloquée pour l'utilisateur {} (score {}, ip {})",
                    utilisateurId, decision.score(), ctx.ip());
        }
        return decision;
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void enregistrerEchec(Long utilisateurId, MethodeConnexion methode, ContexteConnexion ctx, String motif) {
        tentativeRepository.save(TentativeConnexion.builder()
                .utilisateur(utilisateurId == null ? null : utilisateurRepository.getReferenceById(utilisateurId))
                .dateTentative(LocalDateTime.now())
                .methode(methode)
                .succes(false)
                .ip(ctx.ip())
                .appareil(empreinteAppareil(ctx.userAgent()))
                .action(motif)
                .build());
    }

    /** Second facteur validé : la connexion devient un succès et l'appareil est désormais connu. */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void enregistrerSucces(Long utilisateurId, MethodeConnexion methode, ContexteConnexion ctx, String action) {
        tentativeRepository.save(TentativeConnexion.builder()
                .utilisateur(utilisateurRepository.getReferenceById(utilisateurId))
                .dateTentative(LocalDateTime.now())
                .methode(methode)
                .succes(true)
                .ip(ctx.ip())
                .appareil(empreinteAppareil(ctx.userAgent()))
                .action(action)
                .build());
    }

    /** Ré-entraînement hebdomadaire (lundi 3 h) sur les connexions réussies des 90 derniers jours. */
    @Scheduled(cron = "0 0 3 * * MON")
    @Transactional(readOnly = true)
    public void reentrainerModele() {
        List<double[]> historique = tentativeRepository
                .findBySuccesTrueAndDateTentativeAfter(LocalDateTime.now().minusDays(90)).stream()
                .filter(t -> t.getEchecs24h() != null)
                .map(t -> new double[]{
                        t.getDateTentative().getHour(),
                        Boolean.TRUE.equals(t.getNouvelAppareil()) ? 1 : 0,
                        0,
                        t.getEchecs24h(),
                        0})
                .toList();
        try {
            iaClient.reentrainerConnexion(historique);
            log.info("Modèle de détection d'anomalies ré-entraîné sur {} connexions", historique.size());
        } catch (IaIndisponibleException e) {
            log.warn("Ré-entraînement du modèle de connexion reporté : {}", e.getMessage());
        }
    }

    private static String empreinteAppareil(String userAgent) {
        try {
            byte[] h = MessageDigest.getInstance("SHA-256").digest(userAgent.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(h);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException(e);
        }
    }
}
